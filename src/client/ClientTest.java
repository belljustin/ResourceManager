package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.rmi.RemoteException;

import server.ResImpl.InvalidTransactionException;
import server.ResInterface.*;
import Test.ClientThread;
import LockManager.DeadlockException;


public class ClientTest {
  static int NUM_CLIENTS = 5;
  static float LOAD = 20; // transactions per second
  static int PERIOD = (int) (1 / LOAD * 1000); // transaction period in milliseconds
  static int TPERIOD = PERIOD * NUM_CLIENTS; // transaction period of each client
  
  
  public static void main(String args[]) {
    ResourceManager middleware = getMiddleware(args);

    LOAD = Float.valueOf(args[2]);
    PERIOD = (int) (1 / LOAD * 1000); // transaction period in milliseconds
    TPERIOD = PERIOD * NUM_CLIENTS; // transaction period of each client
    NUM_CLIENTS = Integer.valueOf(args[3]);
    
    try {
      setup(middleware);
    } catch (RemoteException | DeadlockException | InvalidTransactionException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    
    Vector<String> flights = new Vector<String>();
    flights.add("300");
    flights.add("200");
    String location = "lax";
    
    ExecutorService es = Executors.newCachedThreadPool();
    for (int i=0; i<NUM_CLIENTS; i++) {
      ClientThread ct = new ClientThread(middleware, TPERIOD, flights, location);
      es.execute(ct);
    }

    es.shutdown();
    try {
      es.awaitTermination(30, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void setup(ResourceManager mw)
      throws RemoteException, DeadlockException, InvalidTransactionException {
    int txnId = mw.start();
    mw.addCars(txnId, "lax", 1000, 200);
    mw.addFlight(txnId, 300, 1000, 100);
    mw.addFlight(txnId, 200, 1000, 100);
    mw.addRooms(txnId, "lax", 1000, 200);
    mw.commit(txnId);
  }
  
  public static ResourceManager getMiddleware(String args[]) {
    ResourceManager mw = null;
    
    String server = "localhost";
    if (args.length > 0) {
      server = args[0];
    }

    int port = 1099;
    if (args.length > 1) {
      port = Integer.parseInt(args[1]);
    }
    
    try {
      Registry registry = LocateRegistry.getRegistry(server, port);
      mw = (ResourceManager) registry.lookup("PG12MiddleWare");
      if (mw == null) {
        System.out.println("Couldn't connect to middleware");
        System.exit(-1);
      }
    } catch (Exception e) {    
      System.err.println("Client exception: " + e.toString());
      e.printStackTrace();
    }
    
    return mw;
  }
}
