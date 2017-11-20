// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.ResImpl;

import LockManager.DeadlockException;
import LockManager.LockManager;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import server.ResInterface.IResourceManager;
import server.Resources.Customer;
import server.Resources.RMHashtable;
import server.Resources.RMItem;
import server.Resources.ReservableItem;
import server.Resources.ReservedItem;

public abstract class ResourceManager implements IResourceManager {

  private RMHashtable m_itemHT = new RMHashtable();

  private HashMap<Integer, RMHashtable> TxnWrites = new HashMap<Integer, RMHashtable>();
  private HashMap<Integer, RMHashtable> TxnDeletes = new HashMap<Integer, RMHashtable>();
  private LockManager lm = new LockManager();

  int port;

  private AtomicInteger txnCounter = new AtomicInteger(0);


  public ResourceManager() throws RemoteException {
  }

  void parseArgs(String args[]) throws IllegalArgumentException {
    if (args.length > 1) {
      throw new IllegalArgumentException("Too few arguments for IMiddleWare\n"
          + "Usage: [port]");
    }

    port = 1099;
    if (args.length == 0) {
      port = Integer.parseInt(args[0]);
    }
  }

  /**
   * Starts a new transaction with the given Id.
   *
   * @return txnId
   */
  public int start(int txnId) throws RemoteException {
    // Create an empty write set for this txn
    TxnWrites.put(txnId, new RMHashtable());
    TxnDeletes.put(txnId, new RMHashtable());
    return txnId;
  }

  /**
   * Starts a new transaction.
   *
   * @return txnId
   */
  public int start() throws RemoteException {
    return start(this.txnCounter.getAndIncrement());
  }

  /**
   * Commits a transaction given its Id.
   *
   * @return success
   */
  public boolean commit(int txnID)
      throws InvalidTransactionException, RemoteException {
    synchronized (m_itemHT) {
      // Add all the writes from txn write set to offical HT
      RMHashtable writes = TxnWrites.get(txnID);
      Set<String> keys = writes.keySet();
      for (String key : keys) {
        m_itemHT.put(key, writes.get(key));
      }

      // Delete all the deletes from txn delete set from official HT
      RMHashtable deletes = TxnDeletes.get(txnID);
      keys = deletes.keySet();
      for (String key : keys) {
        m_itemHT.remove(key);
      }
    }

    // TxnWrites.remove(txnID);
    TxnDeletes.remove(txnID);

    lm.UnlockAll(txnID);
    return true;
  }

  /**
   * Abort a transaction given its Id.
   */
  public void abort(int txnID)
      throws InvalidTransactionException, RemoteException {
    // TxnWrites.remove(txnID);
    TxnDeletes.remove(txnID);

    lm.UnlockAll(txnID);
  }


  /**
   * Reads a data item.
   *
   * @return rmItem
   */
  RMItem readData(int txnId, String key) throws DeadlockException {
    lm.Lock(txnId, key, LockManager.READ);

    // First, check that we haven't removed the value
    RMHashtable deletes = TxnDeletes.get(txnId);
    synchronized (deletes) {
      if (deletes.containsKey(key)) {
        return null;
      }
    }

    // Check if we've already written this value in the current transaction
    RMHashtable writes = TxnWrites.get(txnId);
    synchronized (writes) {
      if (writes.containsKey(key))
        return (RMItem) writes.get(key);
    }

    // Otherwise, grab it from the official hashtable
    synchronized (m_itemHT) {
      return (RMItem) m_itemHT.get(key);
    }
  }

  /**
   * Writes a data item.
   */
  void writeData(int txnId, String key, RMItem value) throws DeadlockException {
    lm.Lock(txnId, key, LockManager.WRITE);

    // Remove from the delete set if it was previously added in the current transaction
    RMHashtable deletes = TxnDeletes.get(txnId);
    synchronized (deletes) {
      if (deletes.containsKey(key)) {
        deletes.remove(key);
      }
    }

    // Then write the value to the current transactions write set
    RMHashtable writes = TxnWrites.get(txnId);
    synchronized (writes) {
      writes.put(key, value);
    }
  }


  /**
   * Remove the item out of storage.
   *
   * @return rmItem
   */
  RMItem removeData(int id, String key) throws DeadlockException {
    lm.Lock(id, key, LockManager.WRITE);

    RMItem toReturn = readData(id, key);

    RMHashtable deletes = TxnDeletes.get(id);
    synchronized (deletes) {
      deletes.put(key, null);
    }

    return toReturn;
  }


  /**
   * Deletes the entire item.
   *
   * @return success
   */
  boolean deleteItem(int id, String key) throws DeadlockException {
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


  /**
   * Query the number of available seats/rooms/cars>
   *
   * @return numAvailable
   */
  int queryNum(int id, String key) throws DeadlockException {
    Trace.info("RM::queryNum(" + id + ", " + key + ") called");
    ReservableItem curObj = (ReservableItem) readData(id, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getCount();
    } // else
    Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
    return value;
  }

  /**
   * Query the price.
   *
   * @return price
   */
  int queryPrice(int id, String key) throws DeadlockException {
    Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
    ReservableItem curObj = (ReservableItem) readData(id, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getPrice();
    } // else
    Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value);
    return value;
  }

  /**
   * Reserve an item.
   *
   * @return success
   */
  boolean reserveItem(int id, int customerID, String key, String location)
      throws DeadlockException {
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

  /**
   * Returns data structure containing customer reservation info.
   *
   * Returns null if the customer doesn't exist. Resturns empty RMHashtable if customer exists but
   * has no reservations.
   *
   * @return reservations
   */
  public RMHashtable getCustomerReservations(int id, int customerID)
      throws RemoteException, DeadlockException {
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

  /**
   * Return a bill
   *
   * @return bill
   */
  public String queryCustomerInfo(int id, int customerID)
      throws RemoteException, DeadlockException {
    Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
    Customer cust = (Customer) readData(id, Customer.getKey(customerID));
    if (cust == null) {
      Trace.warn(
          "RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
      return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
    } else {
      String s = cust.printBill();
      Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows...");
      System.out.println(s);
      return s;
    } // if
  }

  // customer functions

  /**
   * newCustomer returns a unique customer identifier.
   *
   * @return custId
   */
  public int newCustomer(int id)
      throws RemoteException, DeadlockException {
    Trace.info("INFO: RM::newCustomer(" + id + ") called");
    // Generate a globally unique ID for the new customer
    int cid = Integer.parseInt(String.valueOf(id) +
        String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
        String.valueOf(Math.round(Math.random() * 100 + 1)));
    Customer cust = new Customer(cid);
    writeData(id, cust.getKey(), cust);
    Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
    return cid;
  }

  /**
   * Same as newCustomer except with a client provided Id.
   *
   * @return custId
   */
  public boolean newCustomer(int id, int customerID)
      throws RemoteException, DeadlockException {
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


  /**
   * Deletes customer from the database.
   *
   * @return success
   */
  public boolean deleteCustomer(int id, int customerID)
      throws RemoteException, DeadlockException {
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
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem
            .getKey() + " " + reserveditem.getCount() + " times");
        ReservableItem item = (ReservableItem) readData(id, reserveditem.getKey());
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem
            .getKey() + "which is reserved" + item.getReserved() + " times and is still available "
            + item.getCount() + " times");
        item.setReserved(item.getReserved() - reserveditem.getCount());
        item.setCount(item.getCount() + reserveditem.getCount());
      }

      // remove the customer from the storage
      removeData(id, cust.getKey());

      Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
      return true;
    } // if
  }
}
