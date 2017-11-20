// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------
package server.ResImpl;

import java.util.Enumeration;
import java.util.Hashtable;


// A specialization of Hashtable with some
//  extra diagnostics
public class RMHashtable extends Hashtable {

  public RMHashtable() {
    super();
  }

  public String toString() {
    String s = "--- BEGIN RMHashtable ---\n";
    Object key = null;
    for (Enumeration e = keys(); e.hasMoreElements(); ) {
      key = e.nextElement();
      String value = (String) get(key);
      s = s + "[KEY='" + key + "']" + value + "\n";
    }
    s = s + "--- END RMHashtable ---";
    return s;
  }

  public void dump() {
    System.out.println(toString());
  }

  public RMHashtable deepCopy() {
    RMHashtable copy = new RMHashtable();
    Object key = null;
    for (Enumeration e = keys(); e.hasMoreElements(); ) {
      key = e.nextElement();
      copy.put(key, this.get(key));
    }
    return copy;
  }
}
