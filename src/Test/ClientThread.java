package Test;

import LockManager.DeadlockException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Vector;
import server.Transactions.InvalidTransactionException;
import server.ResInterface.IMiddleWare;

public class ClientThread implements Runnable {

  private Thread t;

  private IMiddleWare mw;
  private int period;
  private int ROUNDS = 10;

  private Vector<String> flights;
  private String location;

  private Random rand;

  public ClientThread(IMiddleWare mw, int period, Vector<String> flights, String location) {
    this.mw = mw;
    this.period = period;
    this.flights = flights;
    this.location = location;

    this.rand = new Random();
  }

  public void start() {
    if (t == null) {
      Thread t = new Thread(this);
      t.start();
    }
  }

  @Override
  public void run() {
    for (int i = 0; i < ROUNDS; i++) {
      long waitTime = (long) rand.nextInt(period);
      try {
        Thread.sleep(waitTime * 2);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      long start = System.nanoTime() / 1000;
      try {
        while (ItineraryTransaction() == false) {
          ;
        }
        long duration = System.nanoTime() / 1000 - start;
        System.out.println(duration);
      } catch (Exception e) {
        System.out.println("ItineraryTransaction failed");
        e.printStackTrace();
      }
    }
  }

  public boolean ItineraryTransaction()
      throws RemoteException, InvalidTransactionException {
    int txnId = mw.start();
    try {
      int custId = mw.newCustomer(txnId);
      mw.itinerary(txnId, custId, this.flights, this.location, true, true);
      mw.commit(txnId);
    } catch (DeadlockException e) {
      mw.abort(txnId);
      System.out.println("Deadlock!");
      return false;
    }
    return true;
  }
}
