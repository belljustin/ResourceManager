package client;

import java.util.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;


public class TCPClient {
  static String message = "blank";
  
  protected Socket socket;

  PrintWriter out;
  BufferedReader in;
  BufferedReader stdIn;
  
  public TCPClient(String hostname, int port) throws UnknownHostException, IOException {
    socket = new Socket(hostname, port);
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }
  
  @Override protected void finalize() throws Throwable {
    socket.close();
    super.finalize();
  }

  public static void main(String args[]) {
    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    String command = "";
    Vector<String> arguments = new Vector<String>();
    TCPClient client = null;

    if (args.length != 2) {
      System.err.println("Usage: java client.TCPClient <host name> <port number>");
      System.exit(1);
    }

    String hostName = args[0];
    int portNumber = Integer.parseInt(args[1]);

    try {
      client = new TCPClient(hostName, portNumber);
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + hostName);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to " + hostName);
      System.exit(1);
    }

    System.out.println("\n\n\tClient Interface");
    System.out.println("Type \"help\" for list of supported commands");
    while (true) {
      System.out.print("\n>");
      try {
        // read the next command
        command = stdin.readLine();
      } catch (IOException io) {
        System.out.println("Unable to read from standard in");
        System.exit(1);
      }
      // remove heading and trailing white space
      command = command.trim();
      arguments = client.parse(command);

      // decide which of the commands this was
      switch (client.findChoice((String) arguments.elementAt(0))) {
        case 1: // help section
          client.help(arguments);
          break;

        case 2: // new flight
          client.newFlight(arguments);
          break;

        case 3: // new Car
          client.newCar(arguments);
          break;

        case 4: // new Room
          client.newRoom(arguments);
          break;

        case 5: // new Customer
          client.newCustomer(arguments);
          break;

        case 6: // delete Flight
          client.deleteFlight(arguments);
          break;

        case 7: // delete Car
          client.deleteCar(arguments);
          break;

        case 8: // delete Room
          client.deleteRoom(arguments);
          break;

        case 9: // delete Customer
          client.deleteCustomer(arguments);
          break;

        case 10: // querying a flight
          client.queryFlight(arguments);
          break;

        case 11: // querying a Car Location
          client.queryCarLocation(arguments);
          break;

        case 12: // querying a Room location
          client.queryRoomLocation(arguments);
          break;

        case 13: // querying Customer Information
          client.queryCustomerInfo(arguments);
          break;

        case 14: // querying a flight Price
          client.queryFlightPrice(arguments);
          break;

        case 15: // querying a Car Price
          client.queryCarPrice(arguments);
          break;

        case 16: // querying a Room price
          client.queryRoomPrice(arguments);
          break;

        case 17: // reserve a flight
          client.reserveFlight(arguments);
          break;

        case 18: // reserve a car
          client.reserveCar(arguments);
          break;

        case 19: // reserve a room
          client.reserveRoom(arguments);
          break;

        case 20: // reserve an Itinerary
          client.reserveItinerary(arguments);
          break;

        case 21: // quit the client
           client.quit(arguments);
           break;

        case 22: // new Customer given id
          client.NewCustomerId(arguments);
          break;

        default:
          System.out.println("The interface does not support this command.");
          break;
      }// end of switch
    } // end of while(true)
  }

  public Vector<String> parse(String command) {
    Vector<String> arguments = new Vector<String>();
    StringTokenizer tokenizer = new StringTokenizer(command, ",");
    String argument = "";
    while (tokenizer.hasMoreTokens()) {
      argument = tokenizer.nextToken();
      argument = argument.trim();
      arguments.add(argument);
    }
    return arguments;
  }

  public int findChoice(String argument) {
    if (argument.compareToIgnoreCase("help") == 0)
      return 1;
    else if (argument.compareToIgnoreCase("newflight") == 0)
      return 2;
    else if (argument.compareToIgnoreCase("newcar") == 0)
      return 3;
    else if (argument.compareToIgnoreCase("newroom") == 0)
      return 4;
    else if (argument.compareToIgnoreCase("newcustomer") == 0)
      return 5;
    else if (argument.compareToIgnoreCase("deleteflight") == 0)
      return 6;
    else if (argument.compareToIgnoreCase("deletecar") == 0)
      return 7;
    else if (argument.compareToIgnoreCase("deleteroom") == 0)
      return 8;
    else if (argument.compareToIgnoreCase("deletecustomer") == 0)
      return 9;
    else if (argument.compareToIgnoreCase("queryflight") == 0)
      return 10;
    else if (argument.compareToIgnoreCase("querycar") == 0)
      return 11;
    else if (argument.compareToIgnoreCase("queryroom") == 0)
      return 12;
    else if (argument.compareToIgnoreCase("querycustomer") == 0)
      return 13;
    else if (argument.compareToIgnoreCase("queryflightprice") == 0)
      return 14;
    else if (argument.compareToIgnoreCase("querycarprice") == 0)
      return 15;
    else if (argument.compareToIgnoreCase("queryroomprice") == 0)
      return 16;
    else if (argument.compareToIgnoreCase("reserveflight") == 0)
      return 17;
    else if (argument.compareToIgnoreCase("reservecar") == 0)
      return 18;
    else if (argument.compareToIgnoreCase("reserveroom") == 0)
      return 19;
    else if (argument.compareToIgnoreCase("itinerary") == 0)
      return 20;
    else if (argument.compareToIgnoreCase("quit") == 0)
      return 21;
    else if (argument.compareToIgnoreCase("newcustomerid") == 0)
      return 22;
    else
      return 666;

  }

  public void listCommands() {
    System.out.println("\nWelcome to the client interface provided to test your project.");
    System.out.println("Commands accepted by the interface are:");
    System.out.println("help");
    System.out.println(
        "newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar\ndeleteroom");
    System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
    System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
    System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
    System.out.println("nquit");
    System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
  }


  public void listSpecific(String command) {
    System.out.print("Help on: ");
    switch (findChoice(command)) {
      case 1:
        System.out.println("Help");
        System.out
            .println("\nTyping help on the prompt gives a list of all the commands available.");
        System.out.println(
            "Typing help, <commandname> gives details on how to use the particular command.");
        break;

      case 2: // new flight
        System.out.println("Adding a new Flight.");
        System.out.println("Purpose:");
        System.out.println("\tAdd information about a new flight.");
        System.out.println("\nUsage:");
        System.out.println("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
        break;

      case 3: // new Car
        System.out.println("Adding a new Car.");
        System.out.println("Purpose:");
        System.out.println("\tAdd information about a new car location.");
        System.out.println("\nUsage:");
        System.out.println("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
        break;

      case 4: // new Room
        System.out.println("Adding a new Room.");
        System.out.println("Purpose:");
        System.out.println("\tAdd information about a new room location.");
        System.out.println("\nUsage:");
        System.out.println("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
        break;

      case 5: // new Customer
        System.out.println("Adding a new Customer.");
        System.out.println("Purpose:");
        System.out.println(
            "\tGet the system to provide a new customer id. (same as adding a new customer)");
        System.out.println("\nUsage:");
        System.out.println("\tnewcustomer,<id>");
        break;


      case 6: // delete Flight
        System.out.println("Deleting a flight");
        System.out.println("Purpose:");
        System.out.println("\tDelete a flight's information.");
        System.out.println("\nUsage:");
        System.out.println("\tdeleteflight,<id>,<flightnumber>");
        break;

      case 7: // delete Car
        System.out.println("Deleting a Car");
        System.out.println("Purpose:");
        System.out.println("\tDelete all cars from a location.");
        System.out.println("\nUsage:");
        System.out.println("\tdeletecar,<id>,<location>,<numCars>");
        break;

      case 8: // delete Room
        System.out.println("Deleting a Room");
        System.out.println("\nPurpose:");
        System.out.println("\tDelete all rooms from a location.");
        System.out.println("Usage:");
        System.out.println("\tdeleteroom,<id>,<location>,<numRooms>");
        break;

      case 9: // delete Customer
        System.out.println("Deleting a Customer");
        System.out.println("Purpose:");
        System.out.println("\tRemove a customer from the database.");
        System.out.println("\nUsage:");
        System.out.println("\tdeletecustomer,<id>,<customerid>");
        break;

      case 10: // querying a flight
        System.out.println("Querying flight.");
        System.out.println("Purpose:");
        System.out.println("\tObtain Seat information about a certain flight.");
        System.out.println("\nUsage:");
        System.out.println("\tqueryflight,<id>,<flightnumber>");
        break;

      case 11: // querying a Car Location
        System.out.println("Querying a Car location.");
        System.out.println("Purpose:");
        System.out.println("\tObtain number of cars at a certain car location.");
        System.out.println("\nUsage:");
        System.out.println("\tquerycar,<id>,<location>");
        break;

      case 12: // querying a Room location
        System.out.println("Querying a Room Location.");
        System.out.println("Purpose:");
        System.out.println("\tObtain number of rooms at a certain room location.");
        System.out.println("\nUsage:");
        System.out.println("\tqueryroom,<id>,<location>");
        break;

      case 13: // querying Customer Information
        System.out.println("Querying Customer Information.");
        System.out.println("Purpose:");
        System.out.println("\tObtain information about a customer.");
        System.out.println("\nUsage:");
        System.out.println("\tquerycustomer,<id>,<customerid>");
        break;

      case 14: // querying a flight for price
        System.out.println("Querying flight.");
        System.out.println("Purpose:");
        System.out.println("\tObtain price information about a certain flight.");
        System.out.println("\nUsage:");
        System.out.println("\tqueryflightprice,<id>,<flightnumber>");
        break;

      case 15: // querying a Car Location for price
        System.out.println("Querying a Car location.");
        System.out.println("Purpose:");
        System.out.println("\tObtain price information about a certain car location.");
        System.out.println("\nUsage:");
        System.out.println("\tquerycarprice,<id>,<location>");
        break;

      case 16: // querying a Room location for price
        System.out.println("Querying a Room Location.");
        System.out.println("Purpose:");
        System.out.println("\tObtain price information about a certain room location.");
        System.out.println("\nUsage:");
        System.out.println("\tqueryroomprice,<id>,<location>");
        break;

      case 17: // reserve a flight
        System.out.println("Reserving a flight.");
        System.out.println("Purpose:");
        System.out.println("\tReserve a flight for a customer.");
        System.out.println("\nUsage:");
        System.out.println("\treserveflight,<id>,<customerid>,<flightnumber>");
        break;

      case 18: // reserve a car
        System.out.println("Reserving a Car.");
        System.out.println("Purpose:");
        System.out
            .println("\tReserve a given number of cars for a customer at a particular location.");
        System.out.println("\nUsage:");
        System.out.println("\treservecar,<id>,<customerid>,<location>,<nummberofCars>");
        break;

      case 19: // reserve a room
        System.out.println("Reserving a Room.");
        System.out.println("Purpose:");
        System.out
            .println("\tReserve a given number of rooms for a customer at a particular location.");
        System.out.println("\nUsage:");
        System.out.println("\treserveroom,<id>,<customerid>,<location>,<nummberofRooms>");
        break;

      case 20: // reserve an Itinerary
        System.out.println("Reserving an Itinerary.");
        System.out.println("Purpose:");
        System.out
            .println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
        System.out.println("\nUsage:");
        System.out.println(
            "\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>,<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
        break;


      case 21: // quit the client
        System.out.println("Quitting client.");
        System.out.println("Purpose:");
        System.out.println("\tExit the client application.");
        System.out.println("\nUsage:");
        System.out.println("\tquit");
        break;

      case 22: // new customer with id
        System.out.println("Create new customer providing an id");
        System.out.println("Purpose:");
        System.out.println("\tCreates a new customer with the id provided");
        System.out.println("\nUsage:");
        System.out.println("\tnewcustomerid, <id>, <customerid>");
        break;

      default:
        System.out.println(command);
        System.out.println("The interface does not support this command.");
        break;
    }
  }

  public void wrongNumber() {
    System.out.println("The number of arguments provided in this command are wrong.");
    System.out.println("Type help, <commandname> to check usage of this command.");
  }

  public int getInt(Object temp) throws NumberFormatException {
    try {
      return (new Integer((String) temp)).intValue();
    } catch (NumberFormatException e) {
      throw e;
    }
  }

  public boolean getBoolean(Object temp) throws Exception {
    try {
      return (new Boolean((String) temp)).booleanValue();
    } catch (Exception e) {
      throw e;
    }
  }

  public String getString(Object temp) throws Exception {
    try {
      return (String) temp;
    } catch (Exception e) {
      throw e;
    }
  }

  public void help(Vector<String> args) {
    if (args.size() == 1) // command was "help"
      listCommands();
    else if (args.size() == 2) // command was "help <commandname>"
      listSpecific((String) args.elementAt(1));
    else // wrong use of help command
      System.out.println("Improper use of help command. Type help or help, <commandname>");
  }

  public void newFlight(Vector<String> args) {
    if (args.size() != 5) {
      wrongNumber();
      return;
    }
    System.out.println("Adding a new Flight using id: " + args.elementAt(1));
    System.out.println("Flight number: " + args.elementAt(2));
    System.out.println("Add Flight Seats: " + args.elementAt(3));
    System.out.println("Set Flight Price: " + args.elementAt(4));

    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      getInt(args.elementAt(3));
      getInt(args.elementAt(4));
      
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Flight added");
      else
        System.out.println("Flight could not be added");
    } catch (NumberFormatException e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void newCar(Vector<String> args) {
    if (args.size() != 5) {
      wrongNumber();
      return;
    }
    System.out.println("Adding a new Car using id: " + args.elementAt(1));
    System.out.println("Car Location: " + args.elementAt(2));
    System.out.println("Add Number of Cars: " + args.elementAt(3));
    System.out.println("Set Price: " + args.elementAt(4));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      getInt(args.elementAt(3));
      getInt(args.elementAt(4));

      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Cars added");
      else
        System.out.println("Cars could not be added");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void newRoom(Vector<String> args) {
    if (args.size() != 5) {
      wrongNumber();
      return;
    }
    System.out.println("Adding a new Room using id: " + args.elementAt(1));
    System.out.println("Room Location: " + args.elementAt(2));
    System.out.println("Add Number of Rooms: " + args.elementAt(3));
    System.out.println("Set Price: " + args.elementAt(4));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      getInt(args.elementAt(3));
      getInt(args.elementAt(4));
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Rooms added");
      else
        System.out.println("Rooms could not be added");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void newCustomer(Vector<String> args) {
    if (args.size() != 2) {
      wrongNumber();
      return;
    }
    System.out.println("Adding a new Customer using id:" + args.elementAt(1));
    try {
      getInt(args.elementAt(1));
      String msg = String.join(",", args);
      int customer = sendAndRecvInt(msg);
      System.out.println("new customer id:" + customer);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void deleteFlight(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Deleting a flight using id: " + args.elementAt(1));
    System.out.println("Flight Number: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Flight Deleted");
      else
        System.out.println("Flight could not be deleted");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void deleteCar(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out
        .println("Deleting the cars from a particular location  using id: " + args.elementAt(1));
    System.out.println("Car Location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));

      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Cars Deleted");
      else
        System.out.println("Cars could not be deleted");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void deleteRoom(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out
        .println("Deleting all rooms from a particular location  using id: " + args.elementAt(1));
    System.out.println("Room Location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Rooms Deleted");
      else
        System.out.println("Rooms could not be deleted");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void deleteCustomer(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Deleting a customer from the database using id: " + args.elementAt(1));
    System.out.println("Customer id: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Customer Deleted");
      else
        System.out.println("Customer could not be deleted");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryFlight(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a flight using id: " + args.elementAt(1));
    System.out.println("Flight number: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      
      String msg = String.join(",", args);
      int seats = sendAndRecvInt(msg);
      System.out.println("Number of seats available:" + seats);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryCarLocation(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a car location using id: " + args.elementAt(1));
    System.out.println("Car location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      
      String msg = String.join(",", args);
      int numCars = sendAndRecvInt(msg);
      System.out.println("number of Cars at this location:" + numCars);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryRoomLocation(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a room location using id: " + args.elementAt(1));
    System.out.println("Room location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      
      String msg = String.join(",", args);
      int numRooms = sendAndRecvInt(msg);
      System.out.println("number of Rooms at this location:" + numRooms);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryCustomerInfo(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying Customer information using id: " + args.elementAt(1));
    System.out.println("Customer id: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      
      String msg = String.join(",", args);
      String bill = sendAndRecvStr(msg);
      System.out.println("Customer info:" + bill);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryFlightPrice(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a flight Price using id: " + args.elementAt(1));
    System.out.println("Flight number: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      
      String msg = String.join(",", args);
      int price = sendAndRecvInt(msg);
      System.out.println("Price of a seat:" + price);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryCarPrice(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a car price using id: " + args.elementAt(1));
    System.out.println("Car location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      
      String msg = String.join(",", args);
      int price = sendAndRecvInt(msg);
      System.out.println("Price of a car at this location:" + price);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void queryRoomPrice(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println("Querying a room price using id: " + args.elementAt(1));
    System.out.println("Room Location: " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      getString(args.elementAt(2));
      
      String msg = String.join(",", args);
      int price = sendAndRecvInt(msg);
      System.out.println("Price of Rooms at this location:" + price);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void reserveFlight(Vector<String> args) {
    if (args.size() != 4) {
      wrongNumber();
      return;
    }
    System.out.println("Reserving a seat on a flight using id: " + args.elementAt(1));
    System.out.println("Customer id: " + args.elementAt(2));
    System.out.println("Flight number: " + args.elementAt(3));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      getInt(args.elementAt(3));
      
      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Flight Reserved");
      else
        System.out.println("Flight could not be reserved.");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void reserveCar(Vector<String> args) {
    if (args.size() != 4) {
      wrongNumber();
      return;
    }
    System.out.println("Reserving a car at a location using id: " + args.elementAt(1));
    System.out.println("Customer id: " + args.elementAt(2));
    System.out.println("Location: " + args.elementAt(3));

    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      getString(args.elementAt(3));

      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Car Reserved");
      else
        System.out.println("Car could not be reserved.");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void reserveRoom(Vector<String> args) {
    if (args.size() != 4) {
      wrongNumber();
      return;
    }
    System.out.println("Reserving a room at a location using id: " + args.elementAt(1));
    System.out.println("Customer id: " + args.elementAt(2));
    System.out.println("Location: " + args.elementAt(3));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      getString(args.elementAt(3));

      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Room Reserved");
      else
        System.out.println("Room could not be reserved.");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void reserveItinerary(Vector<String> args) {
    if (args.size() < 7) {
      wrongNumber();
      return;
    }
    System.out.println("Reserving an Itinerary using id:" + args.elementAt(1));
    System.out.println("Customer id:" + args.elementAt(2));
    for (int i = 0; i < args.size() - 6; i++)
      System.out.println("Flight number" + args.elementAt(3 + i));
    System.out.println("Location for Car/Room booking:" + args.elementAt(args.size() - 3));
    System.out.println("Car to book?:" + args.elementAt(args.size() - 2));
    System.out.println("Room to book?:" + args.elementAt(args.size() - 1));
    try {
      getInt(args.elementAt(1));
      getInt(args.elementAt(2));
      Vector<String> flightNumbers = new Vector<String>();
      for (int i = 0; i < args.size() - 6; i++)
        flightNumbers.addElement(args.elementAt(3 + i));
      getString(args.elementAt(args.size() - 3));
      getBoolean(args.elementAt(args.size() - 2));
      getBoolean(args.elementAt(args.size() - 1));

      String msg = String.join(",", args);
      if (sendAndRecv(msg))
        System.out.println("Itinerary Reserved");
      else
        System.out.println("Itinerary could not be reserved.");
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public void quit(Vector<String> args) {
    if (args.size() != 1) {
      wrongNumber();
      return;
    }
    System.out.println("Quitting client.");
    System.exit(1);
  }


  public void NewCustomerId(Vector<String> args) {
    if (args.size() != 3) {
      wrongNumber();
      return;
    }
    System.out.println(
        "Adding a new Customer using id:" + args.elementAt(1) + " and cid " + args.elementAt(2));
    try {
      getInt(args.elementAt(1));
      int Cid = getInt(args.elementAt(2));
      
      String msg = String.join(",", args);
      sendAndRecv(msg);
      System.out.println("new customer id:" + Cid);
    } catch (Exception e) {
      System.out.println("EXCEPTION:");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  public Boolean sendAndRecv(String msg) {
    out.println(msg);
    try {
      return Boolean.getBoolean(in.readLine());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return false;
  }

  public int sendAndRecvInt(String msg) {
    out.println(msg);
    try {
      return Integer.getInteger(in.readLine());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

  public String sendAndRecvStr(String msg) {
    out.println(msg);
    try {
      return in.readLine();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
