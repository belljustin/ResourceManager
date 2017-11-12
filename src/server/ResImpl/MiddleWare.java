package server.ResImpl;

import server.ResInterface.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import LockManager.DeadlockException;
import LockManager.LockManager;
import Test.CSVTestWriter;
import Test.TestData;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddleWare implements ResourceManager 
{
	private ResourceManager rm;
	private ResourceManager flightRM;
	private ResourceManager carRM;
	private ResourceManager hotelRM;
	private Registry registry;
	private ArrayList<TestData> MWTestData = new ArrayList<TestData>();
    
    protected RMHashtable m_itemHT = new RMHashtable();
    protected volatile int txnCounter = 0;
    
    protected HashMap<Integer, RMHashtable> TxnCopies = new HashMap<Integer, RMHashtable>();
    protected HashMap<Integer, RMHashtable> TxnWrites = new HashMap<Integer, RMHashtable>();
    protected HashMap<Integer, RMHashtable> TxnDeletes = new HashMap<Integer, RMHashtable>();
    protected LockManager lm = new LockManager();
    private static final int TIME_TO_LIVE_IN_SECONDS = 360;
    protected ConcurrentHashMap<Integer, Date> TimeToLive = new ConcurrentHashMap<Integer, Date>();
    
    public int start() throws RemoteException {
    	int txnID = txnCounter++;
    	startTime(txnID);

    	// Create a copy of the official HT for this txn
    	TxnCopies.put(txnID, m_itemHT.deepCopy());
    	// Create an empty write set for this txn
    	TxnWrites.put(txnID, new RMHashtable());
    	TxnDeletes.put(txnID, new RMHashtable());
    	
    	// Start the transaction in all the other RMs
    	flightRM.start(txnID);
    	carRM.start(txnID);
    	hotelRM.start(txnID);
    	
    	return txnID;
    }
    
    public void startTime(int txnID){
    	   Calendar now = Calendar.getInstance();
           now.add(Calendar.SECOND, TIME_TO_LIVE_IN_SECONDS);
           Date timeToAdd = now.getTime();
           TimeToLive.put(txnID, timeToAdd);
    }
    
    public void addTime(int txnID){
        if (TimeToLive.get(txnID) != null){
        	startTime(txnID);
        } else{
        	System.out.println("Transaction does not exist!");
        }
    }
    
    public void removeTime(int txnID){
    	TimeToLive.remove(txnID);
    }
    
    public void killTransactions() throws InvalidTransactionException, RemoteException{
    	Iterator it = TimeToLive.entrySet().iterator();
    	System.out.println(TimeToLive.entrySet());
    	while(it.hasNext()){
    		Date currentTime = new Date();
    		ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry) it.next();
    		int compare = currentTime.compareTo((Date) pair.getValue());
    		if(compare > 0){
    			int txnIDtoKill = (int) pair.getKey();
    			System.out.println("Transaction " + txnIDtoKill + " timed out");
    			abort(txnIDtoKill);
    			it.remove();
    		}
    	}
    }
     
    
    // Middleware doesn't start transaction by ID
    public int start(int txnID) throws RemoteException {
    	startTime(txnID);
    	throw new RemoteException("Not implmented");
    }
    
    public boolean commit(int txnID) throws InvalidTransactionException, RemoteException {
    	// Check if the txn exists
    	Date currentStartTime = new Date();
    	if (!TxnCopies.containsKey(txnID)) {
    		throw new InvalidTransactionException(txnID);
    	}

    	synchronized(m_itemHT) {
			// Add all the writes from txn write set to offical HT
			RMHashtable writes = TxnWrites.get(txnID);
			Set<String> keys = writes.keySet();
			for(String key: keys) {
				m_itemHT.put(key, writes.get(key));
			}

			// Delete all the deletes from txn delete set from official HT
			RMHashtable deletes = TxnDeletes.get(txnID);
			keys = deletes.keySet();
			for(String key: keys) {
				m_itemHT.remove(key);
			}
    	}
    	
    	// Remove write set and copy of stale txn
    	TxnCopies.remove(txnID);
    	// TxnWrites.remove(txnID);
    	TxnDeletes.remove(txnID);
    	
    	// Commit the transaction in all the other RMs
    	flightRM.commit(txnID);
    	carRM.commit(txnID);
    	hotelRM.commit(txnID);
    	
    	lm.UnlockAll(txnID);
    	removeTime(txnID);
    	Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(txnID, currentStartTime, currentEndTime, "commit", "Middleware");
    	MWTestData.add(itemToAdd);
    	return true;
    }
    
    public void abort(int txnID) throws InvalidTransactionException, RemoteException {
    	if (!TxnCopies.containsKey(txnID)) {
    		throw new InvalidTransactionException(txnID);
    	}

    	// Remove write set and copy of stale txn
    	TxnCopies.remove(txnID);
    	// TxnWrites.remove(txnID);
    	TxnDeletes.remove(txnID);

    	// Commit the transaction in all the other RMs
    	flightRM.abort(txnID);
    	carRM.abort(txnID);
    	hotelRM.abort(txnID);

    	lm.UnlockAll(txnID);
    	removeTime(txnID);
    }

    public static void main(String args[]) {
        // Figure out where server is running
        String server = "localhost";
        int port = 1099;

        if (args.length == 1) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 &&  args.length != 1) {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java server.ResourceManagerImpl [port]");
            System.exit(1);
        }
        
            MiddleWare middleWare = new MiddleWare();
            middleWare.attachRegistry(port,middleWare);
            try {
            	middleWare.connectRM();
            } catch (Exception e) {
            	System.out.println("Exception: " + e.toString());
            }
            
      	  Thread t1 = new Thread(new Runnable() {
 	         public void run() {
 	              while(true){
 	            	  try {
							middleWare.killTransactions();
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
    
    public void attachRegistry(int port, MiddleWare mw){
        try {
            // create a new Server object
            
            // dynamically generate the stub (client proxy)
            rm = (ResourceManager) UnicastRemoteObject.exportObject(mw, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.getRegistry(port);
            registry.rebind("PG12MiddleWare", rm);
            
            

            System.err.println("MiddleWare ready");
        } catch (Exception e) {
            System.err.println("MiddleWare exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    	
    }
    
    
    public void connectRM() throws Exception{
       	if(registry == null){
    		throw new Exception("Not connected to any registry");
    		
    	}
       	
    	flightRM = (ResourceManager) registry.lookup("PG12FlightRM");
    	carRM = (ResourceManager) registry.lookup("PG12CarRM");
    	hotelRM = (ResourceManager) registry.lookup("PG12HotelRM");
    			
    	
    	if (flightRM == null) {
    		System.out.println("Faliure to connect to FlightRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to FlightRM");
    	
    	}
    	if (carRM == null) {
    		System.out.println("Faliure to connect to CarRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to CarRM");
    	
    	}
    	if (hotelRM == null) {
    		System.out.println("Faliure to connect to HotleRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to HotelRM");
    	
    	}
    	
    	
    			
    }
     
    public MiddleWare() {
    }
    
    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return flightRM.reserveFlight(id, customerID, flightNum);
    }
    
    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, DeadlockException {
    	addTime(id);
    	Date currentStartTime = new Date();
    	boolean valToReturn = flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
    	Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(id, currentStartTime, currentEndTime, "addFlight", "Middleware");
    	MWTestData.add(itemToAdd);
    	return valToReturn;
    }
    
    public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException
    {
    	addTime(id);
    	return flightRM.deleteFlight(id, flightNum);
    }
     

    // Reads a data item
    private RMItem readData( int id, String key ) throws DeadlockException
    {
    	lm.Lock(id, key, LockManager.READ);
    	RMHashtable copy = TxnCopies.get(id);
    	synchronized (m_itemHT) {
    		try {
    			copy.put(key, m_itemHT.get(key));
    		} catch(NullPointerException e) {
    			// key doesn't exist yet
    		}
    	}
		synchronized(copy) {
			return (RMItem) copy.get(key);
		}
    }

    // Writes a data item
    private void writeData( int id, String key, RMItem value ) throws DeadlockException
    {
    	lm.Lock(id, key, LockManager.WRITE);
    	RMHashtable copy = TxnCopies.get(id);
		synchronized(copy) {
			copy.put(key, value);
		}

    	RMHashtable writes = TxnWrites.get(id);
        synchronized(writes) {
            writes.put(key, value);
        }
    }
    
    // Remove the item out of storage
    protected RMItem removeData(int id, String key) throws DeadlockException {
    	lm.Lock(id, key, LockManager.WRITE);
    	RMHashtable deletes = TxnDeletes.get(id);
        synchronized(deletes) {
        	deletes.put(key, null);
        }

    	RMHashtable copy = TxnCopies.get(id);
		synchronized(copy) {
			return (RMItem) copy.remove(key);
		}
    }
    
    
    // deletes the entire item
    protected boolean deleteItem(int id, String key) throws DeadlockException
    {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key );
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getReserved()==0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted" );
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers reserved it" );
                return false;
            }
        } // if
    }
    

    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) throws DeadlockException {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0;  
        if ( curObj != null ) {
            value = curObj.getCount();
        } // else
        Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
        return value;
    }    
    
    // query the price of an item
    protected int queryPrice(int id, String key) throws DeadlockException {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0; 
        if ( curObj != null ) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value );
        return value;        
    }
    
    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key, String location) throws DeadlockException {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );        
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );        
        if ( cust == null ) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } 
        
        // check if the item is available
        ReservableItem item = (ReservableItem)readData(id, key);
        if ( item == null ) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
            return false;
        } else if (item.getCount()==0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
            return false;
        } else {            
            cust.reserve( key, location, item.getPrice());        
            writeData( id, cust.getKey(), cust );
            
            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved()+1);
            
            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " +location+") succeeded" );
            return true;
        }        
    }
    



    




    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, DeadlockException
    {
    	
    	Date currentStartTime = new Date();
    	addTime(id);
    	boolean valToReturn = hotelRM.addRooms(id, location, count, price);
    	Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(id, currentStartTime, currentEndTime, "addRooms", "Middleware");
    	MWTestData.add(itemToAdd);
    	return valToReturn;
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return hotelRM.deleteRooms(id, location);
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, DeadlockException
    {
    	Date currentStarttime = new Date();
    	addTime(id);
    	boolean valToReturn = carRM.addCars(id, location, count, price);
    	Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(id, currentStarttime, currentEndTime, "addCars", "Middleware");
    	MWTestData.add(itemToAdd);
    	return valToReturn;
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return carRM.deleteCars(id, location);
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return flightRM.queryFlight(id, flightNum);
    }

    // Returns the number of reservations for this flight. 
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }


    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return flightRM.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return hotelRM.queryRooms(id, location);
    }


    
    
    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return hotelRM.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException, DeadlockException
    {
    	Date currentStartTime = new Date();
    	addTime(id);
    	int valToReturn = carRM.queryCars(id, location)
        Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(id, currentStartTime, currentEndTime, "queryCars", "Middleware");
    	MWTestData.add(itemToAdd);
    			
    	return valToReturn;
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return carRM.queryCarsPrice(id, location);
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return null;
        } else {
            return cust.getReservations();
        } // if
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException, DeadlockException{
//    {
    	addTime(id);
//        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
//        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
//        if ( cust == null ) {
//            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
//            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
//        } else {
//                String s = cust.printBill();
//                Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
//                System.out.println( s );
//                return s;
//        } // if
    	String toReturn;
        String A = hotelRM.queryCustomerInfo(id, customerID);
    	String B = carRM.queryCustomerInfo(id, customerID);
    	String C = flightRM.queryCustomerInfo(id, customerID);
    	if( A.isEmpty() && B.isEmpty() && C.isEmpty()){
    		Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
    		toReturn = "\n RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" ;
    		return ("\n" + toReturn);
    	}
    	A = ("Hotel: " + A + "\n");
    	B = ("Car: " + B + "\n");
    	C = ("Flight: " + C + "\n");
    	toReturn = A+B+C;

    	return ("\n" + toReturn);
    }

    // customer functions
    // new customer just returns a unique customer identifier
    
    public synchronized int newCustomer(int id)
        throws RemoteException, DeadlockException
    {
    	Date currentStartTime = new Date();
    	addTime(id);
        Trace.info("INFO: RM::newCustomer(" + id + ") called" );
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt( String.valueOf(id) +
                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
        Customer cust = new Customer( cid );
        writeData( id, cust.getKey(), cust );
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
        hotelRM.newCustomer(id, cid);
        carRM.newCustomer(id, cid);
        flightRM.newCustomer(id, cid);
        Date currentEndTime = new Date();
        TestData itemToAdd = new TestData(id, currentStartTime, currentEndTime, "newCustomer", "Middleware");
        MWTestData.add(itemToAdd);
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public synchronized boolean newCustomer(int id, int customerID )
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
            hotelRM.newCustomer(id, customerID);
            carRM.newCustomer(id, customerID);
            flightRM.newCustomer(id, customerID);
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
            return false;
        } // else
    }


    // Deletes customer from the database. 
    public synchronized boolean deleteCustomer(int id, int customerID)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {            
            // Increase the reserved numbers of all reservable items which the customer reserved. 
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {        
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
                ReservableItem item  = (ReservableItem) readData(id, reserveditem.getKey());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
                item.setReserved(item.getReserved()-reserveditem.getCount());
                item.setCount(item.getCount()+reserveditem.getCount());
            }
            
            // remove the customer from the storage
            removeData(id, cust.getKey());
            
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
            hotelRM.deleteCustomer(id, customerID);
            carRM.deleteCustomer(id, customerID);
            flightRM.deleteCustomer(id, customerID);
            return true;
        } // if
    }



    /*
    // Frees flight reservation record. Flight reservation records help us make sure we
    // don't delete a flight if one or more customers are holding reservations
    public boolean freeFlightReservation(int id, int flightNum)
        throws RemoteException
    {
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
        if ( numReservations != null ) {
            numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
        } // if
        writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
                + numReservations + " reservations" );
        return true;
    }
    */

    
    // Adds car reservation to this customer. 
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return carRM.reserveCar(id, customerID, location);
    }


    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, DeadlockException
    {
    	addTime(id);
        return hotelRM.reserveRoom(id, customerID, location);
    }

    
    // Reserve an itinerary 
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException, DeadlockException
    {
    	Date currentStartTime = new Date();
    	addTime(id);
    	boolean flag = true;
    	boolean carFlag = true;
    	boolean hotelFlag = true;
    	Vector<String> flightParams = flightNumbers;
    	for(String flightNumber: flightParams) {
    		int number = Integer.parseInt(flightNumber);
    		boolean temp = flightRM.reserveFlight(id, customer, number);
    		if(temp == false){
    			flag = false;
    		}
    	}
    	if (Car) {
    		carFlag = carRM.reserveCar(id, customer, location);
    	}
    	
    	if (Room) {
    		hotelFlag = hotelRM.reserveRoom(id, customer, location);
    	}
    	
    	
    	Date currentEndTime = new Date();
    	TestData itemToAdd = new TestData(id, currentStartTime, currentEndTime, "itinerary", "Middleware");
    	MWTestData.add(itemToAdd);
     return (flag && carFlag && hotelFlag);
    }
    
    public void writeTestDataToFile(){
    	CSVTestWriter writeMW = new CSVTestWriter("Middleware");
    	writeMW.addData(MWTestData);
    	writeMW.closeFile();
    	carRM.writeTestDataToFile();
    	hotelRM.writeTestDataToFile();
    	flightRM.writeTestDataToFile();
    }
    
//	public Boolean checkNumeric(String msg) {
//		boolean toReturn;
//		try {
//			int x = (int) Integer.parseInt(msg);
//			toReturn = true;
//		} catch (Exception e){
//			toReturn = false;
//		}
//		return toReturn;
//}

}

