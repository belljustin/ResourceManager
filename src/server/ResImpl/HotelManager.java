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
import server.ResInterface.IHotelManager;
import server.Resources.Hotel;

public class HotelManager extends ResourceManager implements IHotelManager {

  private static IHotelManager hmStub;

  public static void main(String args[]) throws RemoteException {
    HotelManager hm = new HotelManager();
    hm.parseArgs(args);

    // create a new Server object
    // dynamically generate the stub (client proxy)
    hmStub = (IHotelManager) UnicastRemoteObject.exportObject(hm, 0);

    // Bind the remote object's stub in the registry
    Registry registry = LocateRegistry.getRegistry(hm.port);
    registry.rebind("PG12HotelRM", hmStub);

    Trace.info("Hotel Server ready");

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
  }

  public HotelManager() throws RemoteException {
    super("PG12HotelRM", false);
  }

  /**
   * Create a new room location or add rooms to an existing location.
   *
   * NOTE: if price <= 0 and the room location already exists, it maintains its current price.
   *
   * @return success
   */
  public boolean addRooms(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
    Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));
    if (curObj == null) {
      // doesn't exist...add it
      Hotel newObj = new Hotel(location, count, price);
      writeData(id, newObj.getKey(), newObj);
      Trace.info(
          "RM::addRooms(" + id + ") created new room location " + location + ", count=" + count
              + ", price=$" + price);
    } else {
      // add count to existing object and update price...
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      } // if
      writeData(id, curObj.getKey(), curObj);
      Trace.info(
          "RM::addRooms(" + id + ") modified existing location " + location + ", count=" + curObj
              .getCount() + ", price=$" + price);
    } // else
    return (true);
  }

  /**
   * Delete rooms from a location.
   *
   * @return success
   */
  public boolean deleteRooms(int id, String location)
      throws RemoteException, DeadlockException {
    return deleteItem(id, Hotel.getKey(location));

  }

  /**
   * Returns the number of rooms available at a location.
   *
   * @return roomsAvailable
   */
  public int queryRooms(int id, String location)
      throws RemoteException, DeadlockException {
    return queryNum(id, Hotel.getKey(location));
  }

  /**
   * Returns room price at this location.
   *
   * @return price
   */
  public int queryRoomsPrice(int id, String location)
      throws RemoteException, DeadlockException {
    return queryPrice(id, Hotel.getKey(location));
  }

  /**
   * Adds room reservation to this customer.
   *
   * @return success
   */
  public boolean reserveRoom(int id, int customerID, String location)
      throws RemoteException, DeadlockException {
    return reserveItem(id, customerID, Hotel.getKey(location), location);
  }

  /**
   * Shutsdown the hotelRM
   */
  public boolean shutdown() throws RemoteException {
    UnicastRemoteObject.unexportObject(hmStub, true);
    return true;
  }
}

