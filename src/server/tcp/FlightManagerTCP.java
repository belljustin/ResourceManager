// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package server.tcp;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import server.ResImpl.Flight;
import server.ResImpl.Trace;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FlightManagerTCP extends ResourceManagerTCP {
  private ServerSocket serverSocket = null;

  private ExecutorService executorService = Executors.newFixedThreadPool(10);

  public static void main(String args[]) {
    int port = 1099;

    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    } else if (args.length > 1) {
      System.err.println("Wrong usage");
      System.out.println("Usage: java server.FlightManagerTCP [port]");
      System.exit(1);
    }

    FlightManagerTCP flightRM = new FlightManagerTCP(port);
    flightRM.runServer();
  }

  public FlightManagerTCP(int port) {
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
      int flightNum;
      int flightSeats;
      int flightPrice;

      String inputLine = null;

      try {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while ((inputLine = in.readLine()) != null) {
          Vector<String> arguments = parse(inputLine);

          String msg = "";
          Trace.info("command: " + arguments.elementAt(0));
          try {
            switch (arguments.elementAt(0)) {
              case "newflight":
                Id = getInt(arguments.elementAt(1));
                flightNum = getInt(arguments.elementAt(2));
                flightSeats = getInt(arguments.elementAt(3));
                flightPrice = getInt(arguments.elementAt(4));
                msg = Boolean.toString(addFlight(Id, flightNum, flightSeats, flightPrice));
                break;

              case "deleteflight":
                Id = getInt(arguments.elementAt(1));
                flightNum = getInt(arguments.elementAt(2));
                msg = Boolean.toString(deleteFlight(Id, flightNum));
                break;

              case "queryflight":
                Id = getInt(arguments.elementAt(1));
                flightNum = getInt(arguments.elementAt(2));
                msg = Integer.toString(queryFlight(Id, flightNum));
                break;

              case "queryflightprice":
                Id = getInt(arguments.elementAt(1));
                flightNum = getInt(arguments.elementAt(2));
                msg = Integer.toString(queryFlightPrice(Id, flightNum));
                break;

              case "reserveflight":
                Id = getInt(arguments.elementAt(1));
                int customer = getInt(arguments.elementAt(2));
                flightNum = getInt(arguments.elementAt(3));
                msg = Boolean.toString(reserveFlight(Id, customer, flightNum));
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

  // Create a new flight, or add seats to existing flight
  // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      {
    Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats
        + ") called");
    Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));
    if (curObj == null) {
      // doesn't exist...add it
      Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
      writeData(id, newObj.getKey(), newObj);
      Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats="
          + flightSeats + ", price=$" + flightPrice);
    } else {
      // add seats to existing flight and update the price...
      curObj.setCount(curObj.getCount() + flightSeats);
      if (flightPrice > 0) {
        curObj.setPrice(flightPrice);
      } // if
      writeData(id, curObj.getKey(), curObj);
      Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats="
          + curObj.getCount() + ", price=$" + flightPrice);
    } // else
    return (true);
  }



  public boolean deleteFlight(int id, int flightNum) {
    return deleteItem(id, Flight.getKey(flightNum));
  }

  // Returns the number of empty seats on this flight
  public int queryFlight(int id, int flightNum) {
    return queryNum(id, Flight.getKey(flightNum));
  }

  // Returns price of this flight
  public int queryFlightPrice(int id, int flightNum) {
    return queryPrice(id, Flight.getKey(flightNum));
  }

  // Adds flight reservation to this customer.
  public boolean reserveFlight(int id, int customerID, int flightNum) {
    return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
  }
}
