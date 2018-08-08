package server.ResImpl;

import LockManager.DeadlockException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import server.ResInterface.ICarManager;
import server.ResInterface.IFlightManager;
import server.ResInterface.IHotelManager;
import server.ResInterface.IMiddleWare;
import server.ResInterface.IResourceManager;
import server.Resources.RMHashtable;
import server.Transactions.InvalidTransactionException;
import server.Transactions.Coordinator;

public class MiddleWare extends ResourceManager implements IMiddleWare {
  public Coordinator coordinator;
  private HealthManager hm;

  private static IMiddleWare mwStub;
  public static Registry registry;

  public IFlightManager flightRM;
  public IHotelManager hotelRM;
  public ICarManager carRM;

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

    mw.startCoordinator();
    mw.coordinator.restoreDecision(); // Check if there was an ongoing decision
  }

  public MiddleWare() throws RemoteException {
    super("PG12MiddleWare", true);
    this.hm = new HealthManager(this);
  }

  private void startCoordinator() throws NotBoundException, RemoteException {
    if (registry == null)
      throw new IllegalStateException("Not connected to any registry");

    try {
      flightRM = (IFlightManager) registry.lookup("PG12FlightRM");
      hotelRM = (IHotelManager) registry.lookup("PG12HotelRM");
      carRM = (ICarManager) registry.lookup("PG12CarRM");

      // Just because they are in the registry doesn't mean they're live
      // Do health pings to check
      flightRM.ping();
      hotelRM.ping();
      carRM.ping();

      Trace.info("Successfully bound to all RMs");
      hm.start(); // Starts a health manager that periodically checks for the liveness of cohorts
    } catch (NotBoundException|RemoteException e) {
      Trace.error("Could not bind to RMs");
      hm.start();
    }

    IResourceManager[] cohorts = {this, flightRM, hotelRM, carRM};
    coordinator = new Coordinator(cohorts);
  }

  /**
   * Transaction Manager methods
   */

  @Override
  public int start() throws RemoteException {
    int txnId = super.start();

    // Start the transaction in all the other RMs
    flightRM.start(txnId);
    hotelRM.start(txnId);
    carRM.start(txnId);

    return txnId;
  }

  @Override
  public synchronized boolean commit(int txnId) throws InvalidTransactionException, RemoteException {
    boolean agreement = coordinator.voteRequest(txnId);
    coordinator.sendDecision(txnId, agreement);
    return agreement;
  }

  @Override
  public void abort(int txnId) throws RemoteException {
    super.abort(txnId);

    // Commit the transaction in all the other RMs
    try {
      flightRM.abort(txnId);
    } catch (RemoteException e) {
      Trace.error("Failed to abort transaction at flightRM");
    }

    try {
      hotelRM.abort(txnId);
    } catch (RemoteException e) {
      Trace.error("Failed to abort transaction at hotelRM");
    }

    try {
      carRM.abort(txnId);
    } catch (RemoteException e) {
      Trace.error("Failed to abort transaction at carRM");
    }
  }


  // Adds flight reservation to this customer.
  public boolean reserveFlight(int id, int customerID, int flightNum)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return flightRM.reserveFlight(id, customerID, flightNum);
  }

  // Create a new flight, or add seats to existing flight
  //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);

  }

  public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException {
    tm.addTime(id);
    return flightRM.deleteFlight(id, flightNum);
  }

  // Create a new room location or add rooms to an existing location
  //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
  public boolean addRooms(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return hotelRM.addRooms(id, location, count, price);
  }

  // Delete rooms from a location
  public boolean deleteRooms(int id, String location)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return hotelRM.deleteRooms(id, location);
  }

  // Create a new car location or add cars to an existing location
  //  NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return carRM.addCars(id, location, count, price);
  }

  // Delete cars from a location
  public boolean deleteCars(int id, String location)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return carRM.deleteCars(id, location);
  }

  // Returns the number of empty seats on this flight
  public int queryFlight(int id, int flightNum)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return flightRM.queryFlight(id, flightNum);
  }

  // Returns price of this flight
  public int queryFlightPrice(int id, int flightNum)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return flightRM.queryFlightPrice(id, flightNum);
  }


  // Returns the number of rooms available at a location
  public int queryRooms(int id, String location)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return hotelRM.queryRooms(id, location);
  }


  // Returns room price at this location
  public int queryRoomsPrice(int id, String location) throws RemoteException, DeadlockException {
    tm.addTime(id);
    return hotelRM.queryRoomsPrice(id, location);
  }


  // Returns the number of cars available at a location
  public int queryCars(int id, String location) throws RemoteException, DeadlockException {
    tm.addTime(id);
    return carRM.queryCars(id, location);
  }


  // Returns price of cars at this location
  public int queryCarsPrice(int id, String location) throws RemoteException, DeadlockException {
    tm.addTime(id);
    return carRM.queryCarsPrice(id, location);
  }

  // Returns data structure containing customer reservation info. Returns null if the
  //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
  //  reservations.
  @Override
  public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException, DeadlockException {
    tm.addTime(id);
    return super.getCustomerReservations(id, customerID);
  }

  // return a bill
  @Override
  public String queryCustomerInfo(int id, int customerID) throws RemoteException, DeadlockException {
    tm.addTime(id);
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
    tm.addTime(id);
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
    tm.addTime(id);
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
    tm.addTime(id);
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
    tm.addTime(id);
    return carRM.reserveCar(id, customerID, location);
  }


  // Adds room reservation to this customer.
  public boolean reserveRoom(int id, int customerID, String location)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
    return hotelRM.reserveRoom(id, customerID, location);
  }


  // Reserve an itinerary
  public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car,
      boolean Room)
      throws RemoteException, DeadlockException {
    tm.addTime(id);
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
}
