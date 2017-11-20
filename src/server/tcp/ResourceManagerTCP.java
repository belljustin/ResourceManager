package server.tcp;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import server.ResImpl.*;

public class ResourceManagerTCP {

  protected RMHashtable m_itemHT = new RMHashtable();

  // Reads a data item
  protected RMItem readData(int id, String key) {
    synchronized (m_itemHT) {
      return (RMItem) m_itemHT.get(key);
    }
  }

  // Writes a data item
  protected void writeData(int id, String key, RMItem value) {
    synchronized (m_itemHT) {
      m_itemHT.put(key, value);
    }
  }

  // Remove the item out of storage
  protected RMItem removeData(int id, String key) {
    synchronized (m_itemHT) {
      return (RMItem) m_itemHT.remove(key);
    }
  }


  // deletes the entire item
  protected boolean deleteItem(int id, String key) {
    Trace.info("RM::deleteItem(" + id + ", " + key + ") called");
    ReservableItem curObj = (ReservableItem) readData(id, key);
    // Check if there is such an item in the storage
    if (curObj == null) {
      Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist");
      return false;
    } else {
      if (curObj.getReserved() == 0) {
        removeData(id, curObj.getKey());
        Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted");
        return true;
      } else {
        Trace.info("RM::deleteItem(" + id + ", " + key
            + ") item can't be deleted because some customers reserved it");
        return false;
      }
    } // if
  }


  // query the number of available seats/rooms/cars
  protected int queryNum(int id, String key) {
    Trace.info("RM::queryNum(" + id + ", " + key + ") called");
    ReservableItem curObj = (ReservableItem) readData(id, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getCount();
    } // else
    Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
    return value;
  }

  // query the price of an item
  protected int queryPrice(int id, String key) {
    Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
    ReservableItem curObj = (ReservableItem) readData(id, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getPrice();
    } // else
    Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value);
    return value;
  }

  // reserve an item
  protected boolean reserveItem(int id, int customerID, String key, String location) {
    Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " + key + ", " + location
        + " ) called");
    // Read customer object if it exists (and read lock it)
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", " + location
          + ")  failed--customer doesn't exist");
      return false;
    }

    // check if the item is available
    ReservableItem item = (ReservableItem) readData(id, key);
    if (item == null) {
      Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
          + ") failed--item doesn't exist");
      return false;
    } else if (item.getCount() == 0) {
      Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
          + ") failed--No more items");
      return false;
    } else {
      cust.reserve(key, location, item.getPrice());
      writeData(id, cust.getKey(), cust);

      // decrease the number of available items in the storage
      item.setCount(item.getCount() - 1);
      item.setReserved(item.getReserved() + 1);

      Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
          + ") succeeded");
      return true;
    }
  }


  // Returns data structure containing customer reservation info. Returns null if the
  // customer doesn't exist. Returns empty RMHashtable if customer exists but has no
  // reservations.
  public RMHashtable getCustomerReservations(int id, int customerID) {
    Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called");
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID
          + ") failed--customer doesn't exist");
      return null;
    } else {
      return cust.getReservations();
    } // if
  }

  // return a bill
  public String queryCustomerInfo(int id, int customerID) {
    Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      Trace.warn(
          "RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
      return ""; // NOTE: don't change this--WC counts on this value indicating a customer does not
      // exist...
    } else {
      String s = cust.printBill();
      Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows...");
      System.out.println(s);
      return s;
    } // if
  }

  // customer functions
  // new customer just returns a unique customer identifier

  public int newCustomer(int id) {
    Trace.info("INFO: RM::newCustomer(" + id + ") called");
    // Generate a globally unique ID for the new customer
    int cid = Integer.parseInt(
        String.valueOf(id) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
            + String.valueOf(Math.round(Math.random() * 100 + 1)));
    Customer cust = new Customer(cid);
    writeData(id, cust.getKey(), cust);
    Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
    return cid;
  }

  // I opted to pass in customerID instead. This makes testing easier
  public boolean newCustomer(int id, int customerID) {
    Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      cust = new Customer(customerID);
      writeData(id, cust.getKey(), cust);
      Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer");
      return true;
    } else {
      Trace.info(
          "INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
      return false;
    } // else
  }


  // Deletes customer from the database.
  public boolean deleteCustomer(int id, int customerID) {
    Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      Trace.warn(
          "RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist");
      return false;
    } else {
      // Increase the reserved numbers of all reservable items which the customer reserved.
      RMHashtable reservationHT = cust.getReservations();
      for (Enumeration e = reservationHT.keys(); e.hasMoreElements(); ) {
        String reservedkey = (String) (e.nextElement());
        ReservedItem reserveditem = cust.getReservedItem(reservedkey);
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved "
            + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
        ReservableItem item = (ReservableItem) readData(id, reserveditem.getKey());
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved "
            + reserveditem.getKey() + "which is reserved" + item.getReserved()
            + " times and is still available " + item.getCount() + " times");
        item.setReserved(item.getReserved() - reserveditem.getCount());
        item.setCount(item.getCount() + reserveditem.getCount());
      }

      // remove the customer from the storage
      removeData(id, cust.getKey());

      Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
      return true;
    } // if
  }

  public String customerCases(Vector<String> arguments) {
    int Id;
    int cId;

    String msg = "";
    try {
      switch (arguments.elementAt(0)) {
        case "deletecustomer":
          Id = getInt(arguments.elementAt(1));
          cId = getInt(arguments.elementAt(2));
          msg = Boolean.toString(deleteCustomer(Id, cId));
          break;

        case "newcustomer":
          Id = getInt(arguments.elementAt(1));
          msg = Integer.toString(newCustomer(Id));
          break;

        case "newcustomerid":
          Id = getInt(arguments.elementAt(1));
          cId = getInt(arguments.elementAt(2));
          msg = Boolean.toString(newCustomer(Id, cId));
          break;

        case "querycustomer":
          Id = getInt(arguments.elementAt(1));
          int customer = getInt(arguments.elementAt(2));
          msg = queryCustomerInfo(Id, customer);

        default:
          System.out.println("The interface does not support this command.");
          break;
      }
    } catch (Exception e) {
      // TODO: handle exception
      System.out.println(e.getMessage());
    }

    return msg;
  }


  public static Vector<String> parse(String command) {
    Vector<String> arguments = new Vector<String>();
    StringTokenizer tokenizer = new StringTokenizer(command, ",");
    String argument = "";
    while (tokenizer.hasMoreTokens()) {
      argument = tokenizer.nextToken();
      argument = argument.trim();
      arguments.add(argument);
    }
    return arguments;
  }

  public static int getInt(Object temp) throws Exception {
    try {
      return (new Integer((String) temp)).intValue();
    } catch (Exception e) {
      throw e;
    }
  }

  public static boolean getBoolean(Object temp) throws Exception {
    try {
      return (new Boolean((String) temp)).booleanValue();
    } catch (Exception e) {
      throw e;
    }
  }

  public static String getString(Object temp) throws Exception {
    try {
      return (String) temp;
    } catch (Exception e) {
      throw e;
    }
  }
}
