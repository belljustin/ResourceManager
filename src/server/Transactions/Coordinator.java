package server.Transactions;

import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import server.ResImpl.Trace;
import server.ResInterface.IResourceManager;

public class Coordinator {
  IResourceManager[] cohorts;

  public Coordinator(IResourceManager[] cohorts) {
    this.cohorts = cohorts;
  }

  /**
   * Checks the disk if there was a decision in progress when the Coordinator died.
   *
   * If there was, it will attempt to broadcast the decision now.
   */
  public void restoreDecision() {
    // Check if there were any ongoing decisions that still need to be broadcasted
    try {
      int txnID = DiskManager.readDecision();
      if (txnID < 0) {
        Trace.info("Abort decision found. Resuming decision.");
        sendDecision(-txnID, false);
      } else {
        Trace.info("Commit decision found. Resuming decision.");
        sendDecision(txnID, true);
      }
    } catch (FileNotFoundException e) {
      Trace.info("No ongoing decisions found");
    }

    // Clear all transactions
    for (IResourceManager c : cohorts) {
      try {
        c.clear();
      } catch (RemoteException e) {
        Trace.warn("RM already down. No need to clear transactions.");
      }
    }
  }

  /**
   * Requests a vote from all RMs
   *
   * If any RM fails to reply to a vote request, the method returns false.
   * Otherwise the voteRequest was successful and returns true.
   *
   * @param txnID
   * @return
   * @throws RemoteException
   */
  public boolean voteRequest(int txnID) throws RemoteException {
    for (IResourceManager cohort : cohorts) {
      // TODO: for testing purposes. Allows us time to kill a process before voteRequest complete
      try {
        Thread.sleep(1000); // TODO: for testing purposes
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Trace.info("Requesting vote");
      boolean vote = true;
      try {
        if (!cohort.voteReply(txnID)) { // If one votes abort, return false immediately
          Trace.warn("A cohort voted to abort the transaction");
          return false;
        }
      } catch (RemoteException e) {
        Trace.warn("A cohort voted to abort the transaction. It has failed.");
        return false; // If one of the cohorts are unavailable, return false
      }
    }

    // Return true if all cohorts vote true
    Trace.info("Recieved commit vote from all cohorts");
    return true;
  }

  /**
   * Sends the decision as recieved from a vote request.
   *
   * First it logs that it has started making a decision to disk.
   * Then it repeatedly broadcasts the decision to all RMs until it is successful with all of them.
   * Finally, it removes it's decision from disk.
   *
   * @param txnID
   * @param commit
   */
  public void sendDecision(int txnID, boolean commit) {
    DiskManager.logDecision(txnID, commit);

    String strCommit = "commit";
    if (!commit)
      strCommit = "abort";
    Trace.info("Sending decision to " + strCommit);

    boolean global_ack = false;
    while(!global_ack) {
      // try to receive an acknowledgment from each cohort
      try {
        for (IResourceManager cohort : cohorts) {
          cohort.recvDecision(txnID, commit);
          Trace.info("Sent " + strCommit);
          Thread.sleep(1000); // TODO: For testing so we can kill the process between steps
        }
        global_ack = true;
      } catch (RemoteException e) {
        Trace.error("Decision: One of the resource managers is not available");

        // Wait some time for RM to come back online
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    DiskManager.deleteDecision(commit);
  }

  public void updateCohort(IResourceManager[] cohorts) {
    this.cohorts = cohorts;
  }
}
