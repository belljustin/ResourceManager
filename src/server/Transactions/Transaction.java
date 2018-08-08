package server.Transactions;

import java.util.Enumeration;
import server.Resources.RMHashtable;
import java.util.HashSet;
import server.Resources.RMItem;

public class Transaction {
  public RMHashtable writeSet;
  public HashSet deleteSet;

  public Transaction() {
    writeSet = new RMHashtable();
    deleteSet = new HashSet<String>();
  }

  public RMHashtable getWriteSet() {
    RMHashtable copy = new RMHashtable();
    Object key;

    synchronized (writeSet) {
      for (Enumeration e = writeSet.keys(); e.hasMoreElements(); ) {
        key = e.nextElement();
        copy.put(key, writeSet.get(key));
      }
    }

    return copy;
  }
}
