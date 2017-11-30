// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.ResImpl;

import LockManager.DeadlockException;
import LockManager.LockManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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
  private HashMap<Integer, HashSet> TxnDeletes = new HashMap<Integer, HashSet>();
  private LockManager lm = new LockManager();

  int port;
  private String name;

  private AtomicInteger txnCounter = new AtomicInteger(0);


  public ResourceManager(String name) throws RemoteException {
    this.name = name;
    try {
		if (restore()) {
		  Trace.info(name + " restored from disk");
		} else {
		  writeHT(this.txnCounter.getAndIncrement(), this.m_itemHT);
		  this.m_itemHT.version = 0;
		}
    } catch (IOException e) {
    	Trace.error("Could not read from disk");
    }
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
    // Create an empty write set for this txn
    TxnWrites.put(txnId, new RMHashtable());
    TxnDeletes.put(txnId, new HashSet<String>());
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
    	try {
			updateHT();
			lm.UnlockAll(txnID);
			return true;
    	} catch (IOException e) { 
    		Trace.error("Could not read/write file");
    	}
    }
	return false;
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
    HashSet deletes = TxnDeletes.get(txnId);
    synchronized (deletes) {
      if (deletes.contains(key)) {
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
    HashSet<String> deletes = TxnDeletes.get(txnId);
    synchronized (deletes) {
      if (deletes.contains(key)) {
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

    HashSet deletes = TxnDeletes.get(id);
    synchronized (deletes) {
      deletes.add(key);
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
            .getKey() + "which is reserved" + itemCopy.getReserved() + " times and is still available "
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
   * Persistence
   *
   * Persitence is maintained by writing RM hashtables (HT) to disk.
   * These HTs are maintained in the [RM_name]_0.ser and [RM_name]_1.ser files.
   *
   * These HTs contain version numbers that identify the most up to date record.
   * Every time a record is saved, it's version is incremented.
   *
   * On RM start, it checks for any existing records and uses the one with the largest version
   * number as the master record.
   */

  /**
   * reads HashMap from file.
   *
   * @return success
   */
  public boolean restore() throws IOException {
    String fnameA = String.format("%s_A.ser", this.name);
    String fnameB = String.format("%s_B.ser", this.name);

    // Check that the first file exists, otherwise unable to restore
    File fA = new File(fnameA);
    File fB = new File(fnameB);
    if (fA.exists() && fB.exists()) {
    	RMHashtable htA = readHT(fnameA);
    	RMHashtable htB = readHT(fnameB);
    	
    	if (htA.version < htB.version) {
    		this.m_itemHT = htA;
    	} else {
    		this.m_itemHT = htB;
    	}
    } else if (fA.exists()) {
    	RMHashtable htA = readHT(fnameA);
		this.m_itemHT = htA;
    } else if (fB.exists()) {
    	RMHashtable htB = readHT(fnameB);
		this.m_itemHT = htB;
    } else {
    	return false;
    }
    
    this.txnCounter.set(this.m_itemHT.version);
    return true;
  }
  
  public boolean updateHT() throws IOException {
	String fnameA = String.format("%s_A.ser", this.name);
	String fnameB = String.format("%s_B.ser", this.name);

	// Check that the first file exists, otherwise unable to restore
	File fA = new File(fnameA);
	File fB = new File(fnameB);
	if (fA.exists() && fB.exists()) {
		RMHashtable htA = readHT(fnameA);
		RMHashtable htB = readHT(fnameB);

		if (htA.version > htB.version) {
			this.m_itemHT = htA;
			fB.delete();
		} else {
			this.m_itemHT = htB;
			fA.delete();
		}
	} else {
		Trace.error("Both files should exist!");
		System.exit(-1);
	}

	return true;
  }
  
  public RMHashtable readHT(String fname) throws IOException {
      FileInputStream fis = new FileInputStream(fname);
      ObjectInputStream ois = new ObjectInputStream(fis);
      try {
		  RMHashtable ht = (RMHashtable) ois.readObject();
		  return ht;
      } catch (ClassNotFoundException e) {
    	  Trace.error("Should never get here because we only save HTs to this file");
    	  System.exit(-1);
      }
      return null;
  }
  
  /**
   * 2 Phase commit
   */

  /**
   * Tries to commit and save the transaction to disk 
   * 
   * @param txnID
   * @return commit
   * @throws RemoteException
   * @throws InvalidTransactionException
   */
  public boolean voteReply(int txnID) throws RemoteException {
	RMHashtable shadow = m_itemHT.deepCopy();
	shadow.version = txnID;

	// Add all the writes from txn write set to shadow HT
	RMHashtable writes = TxnWrites.get(txnID);
	Set<String> keys = writes.keySet();
	for (String key : keys) {
	  Trace.info("Adding " + key + " " + writes.get(key));
	  shadow.put(key, writes.get(key));
	}

	// Delete all the deletes from txn delete set from official HT
	HashSet<String> deletes = TxnDeletes.get(txnID);
	for (String key : deletes) {
	  shadow.remove(key);
	}

    // TxnWrites.remove(txnID);
    TxnDeletes.remove(txnID);
	
    return writeHT(txnID, shadow);
  }
  
  public boolean writeHT(int txnID, RMHashtable ht) {
    char version = 'A';
    if (txnID % 2 != 0)
    	version = 'B';
    ht.version = txnID;

    String fname = String.format("%s_%c.ser", this.name, version);
    Trace.info("Creating " + fname);
    Trace.info(ht.toString());
    try {
		FileOutputStream fos = new FileOutputStream(fname);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(ht);
		oos.close();
    } catch (IOException e) {
    	Trace.error("Can't save to disk");
    	return false;
    }

	return true;
	  
  }
  
  public boolean rollback(int txnId) throws IOException {
	  restore();
	  lm.UnlockAll(txnId);
	  return true;
  }
}
