// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.ResImpl;

import LockManager.DeadlockException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Enumeration;
import server.ResInterface.IResourceManager;
import server.Resources.Customer;
import server.Resources.RMHashtable;
import server.Resources.RMItem;
import server.Resources.ReservableItem;
import server.Resources.ReservedItem;
import server.Transactions.InvalidTransactionException;
import server.Transactions.TransactionManager;
import server.Transactions.WalDoesNotExistException;
import server.Transactions.DiskManager;

public abstract class ResourceManager implements IResourceManager {

  TransactionManager tm;
  RMHashtable m_itemHT;

  private String name;
  int port;

  public ResourceManager(String name, boolean ttl) throws RemoteException {
    this.name = name;
    this.m_itemHT = restoreHT();
    this.tm = new TransactionManager(ttl);
  }

  /**
   * Return hashtable from disk if available
   */
  public RMHashtable restoreHT() {
    RMHashtable ht = DiskManager.restore(name);
    if (ht != null)
      return ht;

    // Create a new hashtable and write it to disk
    ht = new RMHashtable();
    DiskManager.writeHT(name + "_A.ser", ht);
    return ht;
  }

  void parseArgs(String args[]) throws IllegalArgumentException {
    port = 1099;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }
  }

  /**
   * Starts a new transaction with the given Id.
   *
   * @return txnId
   */
  public int start(int txnId) throws RemoteException {
    return tm.start(txnId);
  }

  /**
   * Starts a new transaction.
   *
   * @return txnId
   */
  public int start() throws RemoteException {
    return tm.start();
  }

  /**
   * Abort a transaction given its Id.
   */
  public void abort(int txnID) throws RemoteException {
    try {
      tm.remove(txnID);
    } catch (InvalidTransactionException e) {
      Trace.error("ResourceManager: could not remove transaction that doens't exist");
    }
  }

  /**
   * Reads a data item.
   *
   * @return rmItem
   */
  RMItem readData(int txnID, String key) throws DeadlockException {
    try {
      // Try to read item from transaction read set
      RMItem item = tm.read(txnID, key);
      if (item == null)
        item = (RMItem) m_itemHT.get(key);
      return item;
    } catch (IllegalArgumentException e) {
      Trace.warn(
          "ResourceManager: requested a read on an object that was deleted in this transaction");
      return null;
    }
  }

  /**
   * Writes a data item.
   */
  void writeData(int txnId, String key, RMItem value) throws DeadlockException {
    tm.write(txnId, key, value);
  }

  /**
   * Remove the item out of storage.
   *
   * @return rmItem
   */
  RMItem removeData(int id, String key) throws DeadlockException {
    return tm.delete(id, key);
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

      Customer custCopy = cust.deepClone();
      custCopy.reserve(key, location, item.getPrice());
      writeData(id, custCopy.getKey(), custCopy);

      // decrease the number of available items in the storage
      ReservableItem itemCopy = item.deepClone();
      itemCopy.setCount(item.getCount() - 1);
      itemCopy.setReserved(item.getReserved() + 1);
      writeData(id, key, itemCopy);

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
        ReservableItem itemCopy = item.deepClone();

        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem
            .getKey() + "which is reserved" + itemCopy.getReserved()
            + " times and is still available "
            + itemCopy.getCount() + " times");
        itemCopy.setReserved(itemCopy.getReserved() - reserveditem.getCount());
        itemCopy.setCount(itemCopy.getCount() + reserveditem.getCount());
        writeData(id, reserveditem.getKey(), itemCopy);
      }

      // remove the customer from the storage
      removeData(id, cust.getKey());

      Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
      return true;
    } // if
  }

  /**
   * Cohort Methods
   */

  /**
   * Create a Write Ahead Log from the write set of the given transaction and writes it to disk.
   *
   * @param txnID transaction on which to reply vote on
   */
  public boolean voteReply(int txnID) throws RemoteException {
    RMHashtable wal;

    try {
      wal = tm.applyWrites(m_itemHT, txnID);
    } catch (InvalidTransactionException e) {
      Trace.warn("ResourceManager: cannot vote on invalid transaction.");
      return false;
    }

    DiskManager.writeWAL(name, wal);
    return true;
  }

  /**
   * Receives decision for a transaction.
   *
   * If the decision is to commit, we load the Write Ahead Log (WAL) from disk into memory, upgrade
   * the WAL to primary log, and delete the old log.
   *
   * If the decision is to abort, we delete the WAL.
   *
   * Either way, we remove the transaction from the Transaction Manaer, releasing all the locks
   * associated locks in the process.
   *
   * @param txnID transaction identifier
   * @param commit whether or not to commit
   */
  public void recvDecision(int txnID, boolean commit) throws RemoteException {
    try {
      if (commit)
        m_itemHT = DiskManager.getWalAndDelete(name);
      else
        DiskManager.deleteWAL(name);
    } catch (WalDoesNotExistException e) {
      String action = commit ? "commit" : "abort";
      String warning = String.format("%s has already received the decision to %s", name, action);
      Trace.warn(warning);
    }

    try{
      tm.remove(txnID);
    } catch (InvalidTransactionException e) {
      Trace.warn("Transaction has already been removed. This may have been due to cohort failure");
    }
  }

  /**
   * Dummy call to be used as a health-check. No-op
   *
   * @return success
   */
  public boolean ping() {
    return true;
  }

  /**
   * Removes all transactions
   */
  public void clear() {
    tm.clear();
  }
}
