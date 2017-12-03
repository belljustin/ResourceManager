package server.Transactions;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import server.ResImpl.Trace;

public class TTLManager extends Thread {
  private static final int TIME_TO_LIVE_IN_SECONDS = 360;
  private ConcurrentHashMap<Integer, Date> TimeToLive = new ConcurrentHashMap<>();
  private TransactionManager tm;

  public TTLManager(TransactionManager tm) {
    this.tm = tm;
  }

  public void run() {
    while (true) {
      try {
        Thread.sleep(1000);
        killStaleTransactions();
      } catch (RemoteException e) {
        Trace.error("TTLmanager: can't connect to transaction manager");
      } catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
  }

  /**
   * begins a time to live (TTL) for a transaction.
   *
   * Txns are placed in a set that periodically get checked if they've gone stale
   *
   * @param txnID
   */
  public void startTTL(int txnID) {
    Calendar expiry = Calendar.getInstance();
    expiry.add(Calendar.SECOND, TIME_TO_LIVE_IN_SECONDS);
    TimeToLive.put(txnID, expiry.getTime());
  }

  /**
   * Removes all transactions that are older than their time to lives (ttl)
   */
  private void killStaleTransactions() throws RemoteException {
    Iterator<ConcurrentHashMap.Entry<Integer, Date>> it;
    ConcurrentHashMap.Entry<Integer, Date> ttl;

    Date now = new Date();
    it = TimeToLive.entrySet()
        .iterator();

    // loop through the set of TTLs
    while (it.hasNext()) {
      ttl = it.next();

      // if now is past the ttl, kill abort the transaction and remove the ttl from the set
      int compare = now.compareTo(ttl.getValue());
      if (compare > 0) {
        int txnID = ttl.getKey();
        Trace.warn("Transaction " + txnID + " timed out");
        try {
          tm.remove(txnID);
        } catch (InvalidTransactionException e) {
          Trace.error("TTLmanager: atempted to kill a transaction that does not exist");
        }
        it.remove();
      }
    }
  }

  public void removeTTL(int txnID) {
    TimeToLive.remove(txnID);
  }
}
