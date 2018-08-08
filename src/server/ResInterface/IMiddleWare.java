package server.ResInterface;


import LockManager.DeadlockException;
import java.rmi.RemoteException;
import java.util.Vector;
import server.Transactions.InvalidTransactionException;

public interface IMiddleWare extends IFlightManager, ICarManager, IHotelManager {

  /* reserve an itinerary */
  public boolean itinerary(int id, int customer, Vector<String> flightNumbers, String location,
      boolean Car, boolean Room)
      throws RemoteException, DeadlockException;

  public boolean commit(int txnId) throws RemoteException, InvalidTransactionException;
}