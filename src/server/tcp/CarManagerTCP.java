// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.tcp;

import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.ResImpl.Car;
import server.ResImpl.Trace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CarManagerTCP extends ResourceManagerTCP {
	private ServerSocket serverSocket = null;

	private ExecutorService executorService = Executors.newFixedThreadPool(10);

	public static void main(String args[]) {
		int port = 1099;

		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		} else if (args.length > 1) {
			System.err.println("Wrong usage");
			System.out.println("Usage: java server.CarManagerTCP [port]");
			System.exit(1);
		}

		CarManagerTCP carRM = new CarManagerTCP(port);
		carRM.runServer();
	}

	public CarManagerTCP(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.printf("Could not listen on port: %d\n", port);
			System.exit(1);
		}
	}

	public void runServer() {
		while (true) {
			System.out.println("Waiting for request");
			try {
				Socket s = serverSocket.accept();
				System.out.println("Processing request");
				executorService.submit(new ServiceRequest(s));
			} catch (IOException e) {
				System.out.println("Error accepting connection");
				e.printStackTrace();
			}
		}
	}

	class ServiceRequest implements Runnable {
		private int Id;
		private String location;
		private int numCars;
		private int price;

		private Socket socket;
		PrintWriter out;
		BufferedReader in;

		public ServiceRequest(Socket connection) {
			this.socket = connection;
		}

		public void run() {
			String inputLine = null;

			try {
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				inputLine = in.readLine();
			} catch (IOException e) {
				System.out.println("Exception caught when trying to listen on port");
				System.out.println(e.getMessage());
				return;
			}

			Vector<String> arguments = parse(inputLine);

			String msg = "";
			try {
				switch (arguments.elementAt(0)) {
				case "newcar":
					Id = getInt(arguments.elementAt(1));
					location = getString(arguments.elementAt(2));
					numCars = getInt(arguments.elementAt(3));
					price = getInt(arguments.elementAt(4));
					msg = Boolean.toString(addCars(Id, location, numCars, price));
					break;

				case "deletecar":
					Id = getInt(arguments.elementAt(1));
					location = getString(arguments.elementAt(2));
					msg = Boolean.toString(deleteCars(Id, location));
					break;

				case "querycar":
					Id = getInt(arguments.elementAt(1));
					location = getString(arguments.elementAt(2));
					msg = Integer.toString(queryCars(Id, location));
					break;

				case "querycarprice":
					Id = getInt(arguments.elementAt(1));
					location = getString(arguments.elementAt(2));
					msg = Integer.toString(queryCarsPrice(Id, location));
					break;

				case "reservecar":
					Id = getInt(arguments.elementAt(1));
					int customer = getInt(arguments.elementAt(2));
					location = getString(arguments.elementAt(3));
					msg = Boolean.toString(reserveCar(Id, customer, location));
					break;

				default:
					msg = customerCases(arguments);
				}

				out.println(msg);

			} catch (Exception e) {
				// TODO: Handle this exception
				e.printStackTrace();
			}

			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Error closing client connection");
			}
		}
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current
	// price
	public boolean addCars(int id, String location, int count, int price) throws RemoteException {
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car) readData(id, Car.getKey(location));
		if (curObj == null) {
			// car location doesn't exist...add it
			Car newObj = new Car(location, count, price);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count + ", price=$"
					+ price);
		} else {
			// add count to existing car location and update price...
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			} // if
			writeData(id, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj.getCount()
					+ ", price=$" + price);
		} // else
		return (true);
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws RemoteException {
		return deleteItem(id, Car.getKey(location));
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws RemoteException {
		return queryNum(id, Car.getKey(location));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws RemoteException {
		return queryPrice(id, Car.getKey(location));
	}

	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
		return reserveItem(id, customerID, Car.getKey(location), location);
	}
}
