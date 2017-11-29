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
import server.ResInterface.ICarManager;
import server.Resources.Car;

public class CarManager extends ResourceManager implements ICarManager {

  private static ICarManager cmStub;

  public static void main(String args[]) throws RemoteException {
    CarManager cm = new CarManager();
    cm.parseArgs(args);

    // create a new Server object
    // dynamically generate the stub (client proxy)
    cmStub = (ICarManager) UnicastRemoteObject.exportObject(cm, 0);

    // Bind the remote object's stub in the registry
    Registry registry = LocateRegistry.getRegistry(cm.port);
    registry.rebind("PG12CarRM", cmStub);

    Trace.info("Car Server ready");

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
  }

  public CarManager() throws RemoteException {
    super("PG12CarRM");
  }

  /**
   * Create a new car location or add cars to an existing location.
   *
   * NOTE: if price <= 0 and the location already exists, it maintains its current price
   *
   * @return success
   */
  public boolean addCars(int id, String location, int count, int price)
      throws RemoteException, DeadlockException {
    Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
    Car curObj = (Car) readData(id, Car.getKey(location));
    if (curObj == null) {
      // car location doesn't exist...add it
      Car newObj = new Car(location, count, price);
      writeData(id, newObj.getKey(), newObj);
      Trace.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count
          + ", price=$" + price);
    } else {
      // add count to existing car location and update price...
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      } // if
      writeData(id, curObj.getKey(), curObj);
      Trace.info(
          "RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj
              .getCount() + ", price=$" + price);
    } // else
    return (true);
  }


  /**
   * Delete cars from location.
   *
   * @return success
   */
  public boolean deleteCars(int id, String location)
      throws RemoteException, DeadlockException {
    return deleteItem(id, Car.getKey(location));
  }

  /**
   * Returns the number of cars available at a location.
   *
   * @return carsAvailable
   */
  public int queryCars(int id, String location)
      throws RemoteException, DeadlockException {
    return queryNum(id, Car.getKey(location));
  }

  /**
   * Returns price of cars at a location.
   *
   * @return price
   */
  public int queryCarsPrice(int id, String location)
      throws RemoteException, DeadlockException {
    return queryPrice(id, Car.getKey(location));
  }

  /**
   * Adds car reservation to this customer.
   *
   * @return success
   */
  public boolean reserveCar(int id, int customerID, String location)
      throws RemoteException, DeadlockException {
    return reserveItem(id, customerID, Car.getKey(location), location);
  }

  /**
   * Called to shutdown carManager
   */
  public boolean shutdown() throws RemoteException {
    UnicastRemoteObject.unexportObject(cmStub, true);
    return true;
  }
}
