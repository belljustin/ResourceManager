// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.tcp;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import server.ResImpl.Hotel;
import server.ResImpl.Trace;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HotelManagerTCP extends ResourceManagerTCP {
  private ServerSocket serverSocket = null;

  private ExecutorService executorService = Executors.newFixedThreadPool(10);

  public static void main(String args[]) {
    int port = 1099;

    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    } else if (args.length > 1) {
      System.err.println("Wrong usage");
      System.out.println("Usage: java server.HotelManagerTCP [port]");
      System.exit(1);
    }

    CarManagerTCP carRM = new CarManagerTCP(port);
    carRM.runServer();
  }

  public HotelManagerTCP(int port) {
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
    private Socket socket;
    PrintWriter out;
    BufferedReader in;

    public ServiceRequest(Socket connection) {
      this.socket = connection;
    }

    public void run() {
      int Id;
      String location;
      int numRooms;
      int price;
      String inputLine = null;

      try {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while ((inputLine = in.readLine()) != null) {
          Vector<String> arguments = parse(inputLine);

          String msg = "";
          try {
            switch (arguments.elementAt(0)) {
              case "newroom":
                Id = getInt(arguments.elementAt(1));
                location = getString(arguments.elementAt(2));
                numRooms = getInt(arguments.elementAt(3));
                price = getInt(arguments.elementAt(4));
                msg = Boolean.toString(addRooms(Id, location, numRooms, price));
                break;

              case "deleteroom":
                Id = getInt(arguments.elementAt(1));
                location = getString(arguments.elementAt(2));
                msg = Boolean.toString(deleteRooms(Id, location));
                break;

              case "queryroom":
                Id = getInt(arguments.elementAt(1));
                location = getString(arguments.elementAt(2));
                msg = Integer.toString(queryRooms(Id, location));
                break;

              case "queryroomprice":
                Id = getInt(arguments.elementAt(1));
                location = getString(arguments.elementAt(2));
                msg = Integer.toString(queryRoomsPrice(Id, location));
                break;

              case "reserveroom":
                Id = getInt(arguments.elementAt(1));
                int customer = getInt(arguments.elementAt(2));
                location = getString(arguments.elementAt(3));

                msg = Boolean.toString(reserveRoom(Id, customer, location));
                break;

              default:
                msg = customerCases(arguments);
            }
          } catch (Exception e) {
            // TODO: Handle this exception
            e.printStackTrace();
          }

          out.println(msg);
        }
      } catch (IOException e) {
        System.out.println("Exception caught when trying to read or write on socket");
        System.out.println(e.getMessage());
        return;
      }

      try {
        socket.close();
      } catch (IOException e) {
        System.out.println("Error closing client connection");
      }
    }
  }

  // Create a new room location or add rooms to an existing location
  // NOTE: if price <= 0 and the room location already exists, it maintains its current price
  public boolean addRooms(int id, String location, int count, int price) {
    Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
    Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));
    if (curObj == null) {
      // doesn't exist...add it
      Hotel newObj = new Hotel(location, count, price);
      writeData(id, newObj.getKey(), newObj);
      Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count="
          + count + ", price=$" + price);
    } else {
      // add count to existing object and update price...
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      } // if
      writeData(id, curObj.getKey(), curObj);
      Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count="
          + curObj.getCount() + ", price=$" + price);
    } // else
    return (true);
  }

  // Delete rooms from a location
  public boolean deleteRooms(int id, String location) {
    return deleteItem(id, Hotel.getKey(location));

  }

  // Returns the number of rooms available at a location
  public int queryRooms(int id, String location) {
    return queryNum(id, Hotel.getKey(location));
  }

  // Returns room price at this location
  public int queryRoomsPrice(int id, String location) {
    return queryPrice(id, Hotel.getKey(location));
  }

  // Adds room reservation to this customer.
  public boolean reserveRoom(int id, int customerID, String location) {
    return reserveItem(id, customerID, Hotel.getKey(location), location);
  }
}
