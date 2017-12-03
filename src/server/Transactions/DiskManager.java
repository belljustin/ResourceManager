// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.Transactions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import server.ResInterface.IResourceManager;
import server.Resources.RMHashtable;
import server.Transactions.WalDoesNotExistException;

public abstract class DiskManager implements IResourceManager {
  /**
   * Reads and returns a hashtable at a given file name.
   *
   * @return hashtable
   */
  public static RMHashtable readHT(String fname) {
    try {
      FileInputStream fis = new FileInputStream(fname);
      ObjectInputStream ois = new ObjectInputStream(fis);
      RMHashtable ht = (RMHashtable) ois.readObject();
      return ht;
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Could not read ht file at " + fname);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new IllegalStateException("Hashtable file does not contain an RMHashtable");
    }
  }

  /**
   * Writes a given hashtable to the .ser file specified by name
   *
   * @param fname file name of .ser
   * @param ht hashtable to be written
   */
  public static void writeHT(String fname, RMHashtable ht) {
    try {
      FileOutputStream fos = new FileOutputStream(fname);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(ht);
      oos.close();
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Could not write hashtable to " + fname);
    }
  }

  /**
   * Used for restoring from file after failure or reboot.
   *
   * In the event that two records, A & B, exist simultaneously, the non Write Ahead Log (WAL) will
   * be restored.
   *
   * @param name specifies base name of serialization file
   * @return restoredHT
   */
  public static RMHashtable restore(String name) {
    RMHashtable ht;
    String fname;
    File f;

    // Check if A record exists and is not a WAL
    fname = String.format("%s_A.ser", name);
    f = new File(fname);
    if (f.exists()) {
      ht = readHT(fname);
      if (!ht.wal)
        return ht;
    }

    // Check if B record exists and is not a WAL
    fname = String.format("%s_B.ser", name);
    f = new File(fname);
    if (f.exists()) {
      ht = readHT(fname);
      if (!ht.wal)
        return ht;
    }

    // Neither record may exist, in which case return null
    return null;
  }


  /**
   * Writes Write Ahead Log (WAL) by writing record that does not already exist
   *
   * @param name basename of the .ser file
   * @param ht hashtable to be written to WAL
   */
  public static void writeWAL(String name, RMHashtable ht) {
    ht.wal = true;

    String fnameA = String.format("%s_A.ser", name);
    String fnameB = String.format("%s_B.ser", name);

    File f = new File(fnameA);
    if (f.exists())
      writeHT(fnameB, ht);
    else
      writeHT(fnameA, ht);
  }

  /**
   * Deletes Write Ahead Log (WAL) if it exists
   *
   * @param name basename of the .ser file
   */
  public static void deleteWAL(String name) throws WalDoesNotExistException {
    RMHashtable ht;
    String fname;
    File f;

    // Delete record A if it is a WAL
    fname = String.format("%s_A.ser", name);
    f = new File(fname);

    if (f.exists()) {
      ht = readHT(fname);
      if (ht.wal) {
        f.delete();
        return;
      }
    }

    // Delete record B if it is a WAL
    String fnameB = String.format("%s_B.ser", name);
    f = new File(fname);

    if (f.exists()) {
      ht = readHT(fname);
      if (ht.wal) {
        f.delete();
        return;
      }
    }

    throw new WalDoesNotExistException();
  }

  /**
   * Gets the Write Ahead Log (WAL), promotes it to regular log, and deletes the old log
   *
   * Both records, A & B, should exist when this function is called. If they do not exist, this may
   * due to the commit already having been made, in which case this method call becomes a no-op
   */
  public static RMHashtable getWalAndDelete(String name) throws WalDoesNotExistException {
    String fnameA = String.format("%s_A.ser", name);
    String fnameB = String.format("%s_B.ser", name);

    File fA = new File(fnameA);
    File fB = new File(fnameB);

    if (!(fA.exists() && fB.exists()))
      throw new WalDoesNotExistException();

    RMHashtable htA = readHT(fnameA);
    RMHashtable htB = readHT(fnameB);

    if (htA.wal) {
      // If record A is the WAL, delete record B and return A
      fB.delete();
      htA.wal = false;
      writeHT(fnameA, htA);
      return htA;
    } else if (htB.wal) {
      // If record B is the WAL, delete record A and return B
      fA.delete();
      htB.wal = false;
      writeHT(fnameB, htB);
      return htB;
    }

    // Otherwise the WAL does not exist
    throw new WalDoesNotExistException();
  }

  /**
   * Logs the transaction ID to a either commit.log or abort.log depending on the value of commit
   *
   * @param txnID transaction ID to store
   * @param commit whether this is a commit or abort decision
   */
  public static void logDecision(int txnID, boolean commit) {
    String fname = "abort.log";
    if (commit)
      fname = "commit.log";

    File f = new File(fname);
    if (f.exists())
      return;


    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(fname));
      bw.write(txnID);
      bw.close();
    } catch (IOException e) {
      throw new IllegalStateException("Can't write decision log");
    }
  }

  /**
   * Reads decision from commit or abort .log files.
   *
   * Commit transaction IDs are returned as positive integers
   * Abort transactions IDs are returned as negative integers
   *
   * A FileNotFoundException is raised if neither the commit or abort .log files exist.
   *
   * @return txnID
   * @throws FileNotFoundException
   */
  public static int readDecision() throws FileNotFoundException {
    File f = new File("commit.log");
    if (f.exists())
      return readDecision("commit.log");

    f = new File("abort.log");
    if (f.exists())
      return -readDecision("abort.log");

    throw new FileNotFoundException();
  }

  /**
   * Reads decision txnID from given filename
   *
   * @param fname that holds the decision
   * @return
   */
  static int readDecision(String fname) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(fname));
      return br.read();
    } catch (IOException e) {
      throw new IllegalStateException("Can't read decision log");
    }
  }

  /**
   * Delete decision log file.
   *
   * @param commit true if commit log, false otherwise
   */
  public static void deleteDecision(boolean commit) {
    String fname = "abort.log";
    if (commit)
      fname = "commit.log";

    File f= new File(fname);
    f.delete();
  }
}
