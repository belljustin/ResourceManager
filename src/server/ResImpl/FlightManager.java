// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.ResImpl;

import LockManager.DeadlockException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import server.ResInterface.IFlightManager;
import server.Resources.Flight;

public class FlightManager extends ResourceManager implements IFlightManager {

  private static IFlightManager fmStub;

  public static void main(String args[]) throws RemoteException {
    FlightManager fm = new FlightManager();
    fm.parseArgs(args);

    // create a new Server object
    // dynamically generate the stub (client proxy)
    fmStub = (IFlightManager) UnicastRemoteObject.exportObject(fm, 0);

    // Bind the remote object's stub in the registry
    Registry registry = LocateRegistry.getRegistry(fm.port);
    registry.rebind("PG12FlightRM", fmStub);

    Trace.info("Flight Server ready");

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
  }

  public FlightManager() throws RemoteException {
    super("PG12FlightRM");
  }

  /**
   * Add seats to a flight.
   *
   * In general this will be used to create a new flight, but it should be possible to add seats to
   * an existing flight. Adding to an existing flight should overwrite the current price of the
   * available seats.
   *
   * NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
   *
   * @return success.
   */
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException, DeadlockException {
    Trace.info(String.format("RM::addFlight(%d, %d, $%d, %d) called",
        id, flightNum, flightPrice, flightSeats));

    Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));
    if (curObj == null) {
      // doesn't exist...add it
      Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
      writeData(id, newObj.getKey(), newObj);

      Trace.info(String.format("RM::addFlight(%d) created new flight %d seats=%d, price=$%d",
          id, flightNum, flightSeats, flightPrice));
    } else {
      // add seats to existing flight and update the price...
      curObj.setCount(curObj.getCount() + flightSeats);
      if (flightPrice > 0) {
        curObj.setPrice(flightPrice);
      }
      writeData(id, curObj.getKey(), curObj);

      Trace.info(
          String.format("RM::addFlight(%d) modified existing flight %d, seats=%d, price=$%d", id,
              flightNum, curObj.getCount(), flightPrice));
    }
    return true;
  }


  /**
   * Delete the entire flight.
   *
   * deleteflight implies whole deletion of the flight. all seats, all reservations.  If there is a
   * reservation on the flight, then the flight cannot be deleted
   *
   * @return success.
   */
  public boolean deleteFlight(int id, int flightNum)
      throws RemoteException, DeadlockException {
    return deleteItem(id, Flight.getKey(flightNum));
  }

  /**
   * Returns the number of empty seats on this flight.
   *
   * @return emptySeats
   */
  public int queryFlight(int id, int flightNum)
      throws RemoteException, DeadlockException {
    return queryNum(id, Flight.getKey(flightNum));
  }

  /**
   * Returns price of this flight.
   *
   * @return price
   */
  public int queryFlightPrice(int id, int flightNum)
      throws RemoteException, DeadlockException {
    return queryPrice(id, Flight.getKey(flightNum));
  }

  // Adds flight reservation to this customer.

  /**
   * Adds flight reservation to this customer.
   *
   * @return success
   */
  public boolean reserveFlight(int id, int customerID, int flightNum)
      throws RemoteException, DeadlockException {
    return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
  }

  /**
   * Called to shutdown flightManager
   */
  public boolean shutdown() throws RemoteException {
    UnicastRemoteObject.unexportObject(fmStub, true);
    return true;
  }

}
