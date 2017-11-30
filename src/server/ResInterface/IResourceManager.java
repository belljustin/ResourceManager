package server.ResInterface;


import LockManager.DeadlockException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import server.ResImpl.InvalidTransactionException;

public interface IResourceManager extends Remote {

  /* new customer just returns a unique customer identifier */
  public int newCustomer(int id)
      throws RemoteException, DeadlockException;

  /* new customer with providing id */
  public boolean newCustomer(int id, int cid)
      throws RemoteException, DeadlockException;

  /* deleteCustomer removes the customer and associated reservations */
  public boolean deleteCustomer(int id, int customer)
      throws RemoteException, DeadlockException;

  /* return a bill */
  public String queryCustomerInfo(int id, int customer)
      throws RemoteException, DeadlockException;

  public int start(int txnId) throws RemoteException;

  public int start() throws RemoteException;

  public boolean commit(int txnId) throws RemoteException, InvalidTransactionException;

  public void abort(int txnID) throws RemoteException, InvalidTransactionException;

  public boolean shutdown() throws RemoteException;

  public boolean voteReply(int txnID) throws RemoteException;

  public boolean rollback(int txnID) throws IOException;
}
