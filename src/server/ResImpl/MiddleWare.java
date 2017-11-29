package server.ResImpl;

import LockManager.DeadlockException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import server.ResInterface.ICarManager;
import server.ResInterface.IFlightManager;
import server.ResInterface.IHotelManager;
import server.ResInterface.IMiddleWare;
import server.Resources.RMHashtable;

public class MiddleWare extends ResourceManager implements IMiddleWare {

  private IFlightManager flightRM;
  private ICarManager carRM;
  private IHotelManager hotelRM;

  private static Registry registry;
  private static IMiddleWare mwStub;

  private static final int TIME_TO_LIVE_IN_SECONDS = 360;
  private ConcurrentHashMap<Integer, Date> TimeToLive = new ConcurrentHashMap<>();

  public static void main(String args[]) throws RemoteException, NotBoundException {
    MiddleWare mw = new MiddleWare();
    mw.parseArgs(args);

    // create a new Server object
    // dynamically generate the stub (client proxy)
    mwStub = (IMiddleWare) UnicastRemoteObject.exportObject(mw, 0);

    registry = LocateRegistry.getRegistry(mw.port);
    registry.rebind("PG12MiddleWare", mwStub);

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }

    mw.connectRM();

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        while (true) {
          try {
            mw.killTransactions();
            Thread.sleep(1000);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

        }
      }
    });
    t1.start();
  }

  public MiddleWare() throws RemoteException {
    super("PG12MiddleWare");
  }

  private void connectRM()
      throws IllegalStateException, RemoteException, NotBoundException {
    if (registry == null) {
      throw new IllegalStateException("Not connected to any registry");
    }

    flightRM = (IFlightManager) registry.lookup("PG12FlightRM");
    carRM = (ICarManager) registry.lookup("PG12CarRM");
    hotelRM = (IHotelManager) registry.lookup("PG12HotelRM");

    Trace.info("Succesfully bound to Flight, Car, and Hotel RMs");
  }

  /**
   * Time-To-Live Mechanism
   */

  private void startTime(int txnID) {
    Calendar now = Calendar.getInstance();
    now.add(Calendar.SECOND, TIME_TO_LIVE_IN_SECONDS);
    Date timeToAdd = now.getTime();
    TimeToLive.put(txnID, timeToAdd);
    Trace.info("Added txnId " + txnID + " to TTL");
  }

  private void addTime(int txnID) {
    if (TimeToLive.get(txnID) != null) {
      startTime(txnID);
    } else {
      Trace.error("Transaction does not exist!");
    }
  }

  private void removeTime(int txnID) {
    TimeToLive.remove(txnID);
  }

  private void killTransactions() throws InvalidTransactionException, RemoteException {
    Iterator it = TimeToLive.entrySet().iterator();
    while (it.hasNext()) {
      Date currentTime = new Date();
      ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry) it.next();
      int compare = currentTime.compareTo((Date) pair.getValue());
      if (compare > 0) {
        int txnIDtoKill = (int) pair.getKey();
        Trace.info("Transaction " + txnIDtoKill + " timed out");
        abort(txnIDtoKill);
        it.remove();
      }
    }
  }

  /**
   * Transaction Manager methods
   */

  @Override
  public int start() throws RemoteException {
    int txnId = super.start();
    startTime(txnId);

    // Start the transaction in all the other RMs
    flightRM.start(txnId);
    carRM.start(txnId);
    hotelRM.start(txnId);

    return txnId;
  }

  @Override
  public boolean commit(int txnId)
      throws InvalidTransactionException, RemoteException {
    Trace.info("Removing txnId: " + txnId);

    // Check if the txn exists
    if (!TimeToLive.containsKey(txnId)) {
      throw new InvalidTransactionException(txnId);
    }

    // Commit the transaction in all RMs
    flightRM.commit(txnId);
    carRM.commit(txnId);
    hotelRM.commit(txnId);

    // Commit transaction in Middleware
    super.commit(txnId);

    // Remove transaction from time-to-live
    removeTime(txnId);
    return true;
  }

  @Override
  public void abort(int txnId)
      throws InvalidTransactionException, RemoteException {
    Trace.info("Aborting txnId: " + txnId);

    // Check if the txn exists
    if (!TimeToLive.containsKey(txnId)) {
      Trace.info("txnId " + txnId + " does not exist");
    }

    // Commit the transaction in all the other RMs
    flightRM.abort(txnId);
    carRM.abort(txnId);
    hotelRM.abort(txnId);

    super.abort(txnId);
    removeTime(txnId);
  }


  // Adds flight reservation to this customer.
  public boolean reserveFlight(int id, int customerID, int flightNum)
      throws RemoteException, DeadlockException {
    addTime(id);
    return flightRM.reserveFlight(id, customerID, flightNum);
  }

  // Create a new flight, or add seats to existing flight
  //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException, DeadlockException {
    addTime(id);
    return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);

  }

  public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException {
    addTime(id);
    return flightRM.deleteFlight(id, flightNum);
  }

  // Create a new room location or add rooms to an existing location
  //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
  public boolean addRooms(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    addTime(id);
    return hotelRM.addRooms(id, location, count, price);
  }

  // Delete rooms from a location
  public boolean deleteRooms(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return hotelRM.deleteRooms(id, location);
  }

  // Create a new car location or add cars to an existing location
  //  NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    addTime(id);
    return carRM.addCars(id, location, count, price);
  }

  // Delete cars from a location
  public boolean deleteCars(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return carRM.deleteCars(id, location);
  }

  // Returns the number of empty seats on this flight
  public int queryFlight(int id, int flightNum)
      throws RemoteException, DeadlockException {
    addTime(id);
    return flightRM.queryFlight(id, flightNum);
  }

  // Returns price of this flight
  public int queryFlightPrice(int id, int flightNum)
      throws RemoteException, DeadlockException {
    addTime(id);
    return flightRM.queryFlightPrice(id, flightNum);
  }


  // Returns the number of rooms available at a location
  public int queryRooms(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return hotelRM.queryRooms(id, location);
  }


  // Returns room price at this location
  public int queryRoomsPrice(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return hotelRM.queryRoomsPrice(id, location);
  }


  // Returns the number of cars available at a location
  public int queryCars(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return carRM.queryCars(id, location);
  }


  // Returns price of cars at this location
  public int queryCarsPrice(int id, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return carRM.queryCarsPrice(id, location);
  }

  // Returns data structure containing customer reservation info. Returns null if the
  //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
  //  reservations.
  @Override
  public RMHashtable getCustomerReservations(int id, int customerID)
      throws RemoteException, DeadlockException {
    addTime(id);
    return super.getCustomerReservations(id, customerID);
  }

  // return a bill
  @Override
  public String queryCustomerInfo(int id, int customerID)
      throws RemoteException, DeadlockException {
    addTime(id);
    String toReturn;
    String A = hotelRM.queryCustomerInfo(id, customerID);
    String B = carRM.queryCustomerInfo(id, customerID);
    String C = flightRM.queryCustomerInfo(id, customerID);
    if (A.isEmpty() && B.isEmpty() && C.isEmpty()) {
      Trace.warn(
          "RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
      toReturn =
          "\n RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist";
      return ("\n" + toReturn);
    }
    A = ("Hotel: " + A + "\n");
    B = ("Car: " + B + "\n");
    C = ("Flight: " + C + "\n");
    toReturn = A + B + C;

    return ("\n" + toReturn);
  }

  // customer functions
  // new customer just returns a unique customer identifier
  @Override
  public synchronized int newCustomer(int id)
      throws RemoteException, DeadlockException {
    addTime(id);
    int cid = super.newCustomer(id);
    hotelRM.newCustomer(id, cid);
    carRM.newCustomer(id, cid);
    flightRM.newCustomer(id, cid);
    return cid;
  }

  // I opted to pass in customerID instead. This makes testing easier
  @Override
  public synchronized boolean newCustomer(int id, int customerID)
      throws RemoteException, DeadlockException {
    addTime(id);
    boolean success = super.newCustomer(id, customerID);

    if (!success) {
      Trace.info(
          "INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
      return false;
    }

    hotelRM.newCustomer(id, customerID);
    carRM.newCustomer(id, customerID);
    flightRM.newCustomer(id, customerID);
    return true;

  }


  // Deletes customer from the database.
  @Override
  public synchronized boolean deleteCustomer(int id, int customerID)
      throws RemoteException, DeadlockException {
    addTime(id);
    boolean success = super.deleteCustomer(id, customerID);

    if (!success) {
      Trace.warn(
          "RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist");
      return false;
    }

    hotelRM.deleteCustomer(id, customerID);
    carRM.deleteCustomer(id, customerID);
    flightRM.deleteCustomer(id, customerID);
    return true;
  }

  // Adds car reservation to this customer.
  public boolean reserveCar(int id, int customerID, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return carRM.reserveCar(id, customerID, location);
  }


  // Adds room reservation to this customer.
  public boolean reserveRoom(int id, int customerID, String location)
      throws RemoteException, DeadlockException {
    addTime(id);
    return hotelRM.reserveRoom(id, customerID, location);
  }


  // Reserve an itinerary
  public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car,
      boolean Room)
      throws RemoteException, DeadlockException {
    addTime(id);
    boolean flag = true;
    boolean carFlag = true;
    boolean hotelFlag = true;
    Vector<String> flightParams = flightNumbers;
    for (String flightNumber : flightParams) {
      int number = Integer.parseInt(flightNumber);
      boolean temp = flightRM.reserveFlight(id, customer, number);
      if (temp == false) {
        flag = false;
      }
    }
    if (Car) {
      carFlag = carRM.reserveCar(id, customer, location);
    }

    if (Room) {
      hotelFlag = hotelRM.reserveRoom(id, customer, location);
    }

    return (flag && carFlag && hotelFlag);
  }

  public boolean shutdown() throws RemoteException {
    while (!TimeToLive.isEmpty())

    {
      hotelRM.shutdown();
    }
    carRM.shutdown();
    hotelRM.shutdown();

    UnicastRemoteObject.unexportObject(mwStub, true);
    return true;
  }
}
