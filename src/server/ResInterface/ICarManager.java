package server.ResInterface;


import LockManager.DeadlockException;
import java.rmi.RemoteException;

public interface ICarManager extends IResourceManager {

  /* Add cars to a location.
   * This should look a lot like addFlight, only keyed on a string location
   * instead of a flight number.
   */
  public boolean addCars(int id, String location, int numCars, int price)
      throws RemoteException, DeadlockException;

  /* Delete all Cars from a location.
   * It may not succeed if there are reservations for this location
   *
   * @return success
   */
  public boolean deleteCars(int id, String location)
      throws RemoteException, DeadlockException;

  /* return the number of cars available at a location */
  public int queryCars(int id, String location)
      throws RemoteException, DeadlockException;

  /* return the price of a car at a location */
  public int queryCarsPrice(int id, String location)
      throws RemoteException, DeadlockException;

  /* reserve a car at this location */
  public boolean reserveCar(int id, int customer, String location)
      throws RemoteException, DeadlockException;
}
