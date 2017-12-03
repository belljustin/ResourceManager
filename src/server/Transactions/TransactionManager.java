// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.Transactions;

import LockManager.LockManager;
import LockManager.DeadlockException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import server.Resources.RMHashtable;
import server.Resources.RMItem;

public class TransactionManager {
  private HashMap<Integer, Transaction> transactions;
  private AtomicInteger txnCounter;
  private TTLManager ttlManager;
  private LockManager lm;


  // TODO: Deal with missing transactions
  public TransactionManager(boolean ttl) throws RemoteException {
    txnCounter = new AtomicInteger(1);
    ttlManager = new TTLManager(this);
    transactions = new HashMap<>();
    lm = new LockManager();

    // Make time-to-live optional so we can decide only to manage it at the MiddleWare
    if (ttl) {
      ttlManager.start();
    }
  }

  /**
   * Starts a new transaction with the given ID.
   *
   * @return txnID
   */
  public int start(int txnID) {
    Transaction txn  = new Transaction();
    transactions.put(txnID, txn);
    ttlManager.startTTL(txnID);
    return txnID;
  }

  /**
   * Start a new transaction using the counter.
   *
   * @return txnID
   */
  public int start() {
    return start(txnCounter.getAndIncrement());
  }

  /**
   * remove a transaction given its Id.
   */
  public void remove(int txnID) throws InvalidTransactionException, RemoteException {
    if (!transactions.containsKey(txnID))
      throw new InvalidTransactionException(txnID);

    ttlManager.removeTTL(txnID);
    transactions.remove(txnID);
    lm.UnlockAll(txnID);
  }

  /**
   *
   * @param txnID transaction identifier
   * @param key item key
   *
   * @return the RM item or null if it does not exist
   * @throws DeadlockException
   * @throws IllegalArgumentException thrown when the item was deleted in this transaction
   */
  public RMItem read(int txnID, String key) throws DeadlockException, IllegalArgumentException {
    lm.Lock(txnID, key, LockManager.READ);
    Transaction txn = transactions.get(txnID);

    // First, check that we haven't deleted the item yet
    if (txn.deleteSet.contains(key)) {
      throw new IllegalArgumentException(
          "TransactionManager: requested a read on an object that was deleted in this transaction");
    }

    RMHashtable writes = txn.getWriteSet();
    if (writes.containsKey(key))
      return (RMItem) writes.get(key);

    return null;
  }

  /**
   * Writes a data item.
   */
  public void write(int txnId, String key, RMItem value) throws DeadlockException {
    lm.Lock(txnId, key, LockManager.WRITE);
    Transaction txn = transactions.get(txnId);

    // Remove from the delete set if it was previously added in the current transaction
    synchronized (txn.deleteSet) {
      if (txn.deleteSet.contains(key))
        txn.deleteSet.remove(key);
    }

    // Then write the value to the current transactions write set
    txn.writeSet.put(key, value);
  }

  /**
   * Remove item from storage
   */
  public RMItem delete(int id, String key) throws DeadlockException {
    lm.Lock(id, key, LockManager.WRITE);

    RMItem toReturn = read(id, key);
    Transaction txn = transactions.get(id);
    txn.deleteSet.add(key);

    return toReturn;
  }


  /**
   * Returns a newly created hashtable with all the txn write & delete sets applied.
   *
   * @param ht the RMHashtable to apply the write set on.
   * @param txnID the transaction identifier
   * @return new hashtable with applied write set
   */
  public RMHashtable applyWrites(RMHashtable ht, int txnID) throws InvalidTransactionException {
    RMHashtable copyHt = ht.deepCopy();
    Transaction txn = transactions.get(txnID);

    // Add all the writes from txn write set to shadow HT
    RMHashtable writes = txn.getWriteSet();
    for (Object k : writes.keySet()) {
      RMItem item = (RMItem) writes.get(k);
      copyHt.put(k, item);
    }

    // Delete all the deletes from txn delete set from official HT
    HashSet<String> deletes = txn.deleteSet;
    for (String key : deletes)
      copyHt.remove(key);

    return copyHt;
  }

  public void addTime(int txnID) {
    ttlManager.startTTL(txnID);
  }
}
