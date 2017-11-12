package client;

import server.ResImpl.InvalidTransactionException;
import server.ResInterface.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;
import LockManager.DeadlockException;
import java.io.*;

    
public class ClientTest
{
    static String message = "blank";
    static ResourceManager rm = null;
    
    static int txnId;

    public static void main(String args[])
    {
        ClientTest obj = new ClientTest();
        txnId = -1;


        String server = "localhost";
        int port = 1099;
        if (args.length > 0)
        {
            server = args[0];
        }
        if (args.length > 1)
        {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 2)
        {
            System.out.println ("Usage: java client [rmihost [rmiport]]");
            System.exit(1);
        }
        
        try 
        {
            // get a reference to the rmiregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by rmiregistry lookup
           // rm = (ResourceManager) registry.lookup("PG12ResourceManager");
            rm = (ResourceManager) registry.lookup("PG12MiddleWare");
            if(rm!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to RM");
            }
            else
            {
                System.out.println("Unsuccessful");
            }
            // make call on remote method
        } 
        catch (Exception e) 
        {    
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        
        
        
        if (System.getSecurityManager() == null) {
            //System.setSecurityManager(new RMISecurityManager());
        }
        
        try {
			carRMtest(rm);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        try {
			FullTest(rm);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void carRMtest(ResourceManager rm)
    		throws RemoteException, DeadlockException, InvalidTransactionException {
        txnId = rm.start();
        rm.addCars(txnId, "lax", 20, 200);
        rm.addCars(txnId, "mtl", 20, 200);
        rm.addCars(txnId, "tor", 20, 200);
        rm.addCars(txnId, "van", 20, 200);
        rm.queryCars(txnId, "lax");
        rm.commit(txnId);
        rm.writeTestDataToFile();
    }
    
    public static void FullTest(ResourceManager rm)
    		throws RemoteException, DeadlockException, InvalidTransactionException {
    	
    	// Setup
    	txnId = rm.start();
    	rm.addCars(txnId, "lax", 20, 200);
    	rm.addFlight(txnId, 300, 20, 100);
    	rm.addFlight(txnId, 200, 20, 100);
    	rm.addRooms(txnId, "lax", 20, 200);
    	rm.commit(txnId);
    	
    	Vector<String> flights = new Vector<String>();
    	flights.add("300");
    	flights.add("200");

    	// Actual Test
    	txnId =  rm.start();
    	rm.newCustomer(txnId, 1);
    	rm.itinerary(txnId, 1, flights, "lax", true, true);
    	rm.commit(txnId);
    	rm.writeTestDataToFile();
    }
}
