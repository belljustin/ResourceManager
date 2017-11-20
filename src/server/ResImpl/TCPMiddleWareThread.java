package server.ResImpl;

import server.ResInterface.*;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;


public class TCPMiddleWareThread implements Runnable {

  Socket socket;
  String carRMHostName;
  String hotelRMHostName;
  String flightRMHostName;
  int carRMPort;
  int flightRMPort;
  int hotelRMPort;

  TCPMiddleWareThread(Socket socket, String aCarRMHostName, String aHotelRMHostName,
      String aFlightRMHostName, int aCarRMPort, int aFlightRMPort, int aHotelRMPort) {
    this.socket = socket;
    carRMHostName = aCarRMHostName;
    hotelRMHostName = aHotelRMHostName;
    flightRMHostName = aFlightRMHostName;
    carRMPort = aCarRMPort;
    flightRMPort = aFlightRMPort;
    hotelRMPort = aHotelRMPort;

  }

  public void run() {
    try {

      Socket carRMSocket = new Socket(carRMHostName, carRMPort);
      PrintWriter outToCarRM = new PrintWriter(carRMSocket.getOutputStream(), true);
      BufferedReader inFromCarRM = new BufferedReader(
          new InputStreamReader(carRMSocket.getInputStream()));

      Socket hotelRMSocket = new Socket(hotelRMHostName, hotelRMPort);
      PrintWriter outToHotelRM = new PrintWriter(hotelRMSocket.getOutputStream(), true);
      BufferedReader inFromHotelRM = new BufferedReader(
          new InputStreamReader(hotelRMSocket.getInputStream()));

      Socket flightRMSocket = new Socket(flightRMHostName, flightRMPort);
      PrintWriter outToFlightRM = new PrintWriter(flightRMSocket.getOutputStream(), true);
      BufferedReader inFromFlightRM = new BufferedReader(
          new InputStreamReader(flightRMSocket.getInputStream()));

      BufferedReader inFromClient = new BufferedReader(
          new InputStreamReader(socket.getInputStream()));
      PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);

      String message = null;
      String messageToClient = null;
      while ((message = inFromClient.readLine()) != null) {
        Trace.info("Recieved command: " + message);
        String[] params = message.split(",");
        String id = params[1];
        switch (forwardRM(params[0])) {
          case 0:
            messageToClient = sendAndRecvStr(message, outToFlightRM, inFromFlightRM);
            //forward to Flight RM
            break;
          case 1:
            Trace.info("Sending command to carRM" + message);
            messageToClient = sendAndRecvStr(message, outToCarRM, inFromCarRM);
            Trace.info("Recieved: " + messageToClient);
            //forward to CarRM
            break;
          case 2:
            messageToClient = sendAndRecvStr(message, outToHotelRM, inFromHotelRM);
            //forward to HotelRM
            break;
          case 3:
            int cid;
            if (params[0].compareToIgnoreCase("newcustomer") == 0) {
              cid = Integer.parseInt(String.valueOf(id) +
                  String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                  String.valueOf(Math.round(Math.random() * 100 + 1)));
              message = "newcustomerid," + params[1] + "," + cid;
            } else if (params[0].compareToIgnoreCase("newcustomerid") == 0) {
              cid = Integer.parseInt(params[2]);
            } else {
              cid = Integer.parseInt(params[2]);
            }

            String temp_message_Flight = sendAndRecvStr(message, outToFlightRM, inFromFlightRM);
            String temp_message_Car = sendAndRecvStr(message, outToCarRM, inFromCarRM);
            String temp_message_Hotel = sendAndRecvStr(message, outToHotelRM, inFromHotelRM);
            messageToClient = "" + cid;
            //forward to FlightRM
            //forward to CarRM
            //forward to HotelRM
            break;
          case 4:
            String customerID = params[2];
            Vector<String> flights = new Vector<String>();
            int i = 3;
            while (checkNumeric(params[i])) {
              flights.add(params[i]);
              i++;
            }
            for (String flight : flights) {
              System.out.println(flight);
            }
            String location = params[i];
            i++;
            boolean bookCar = Boolean.parseBoolean(params[i]);
            i++;
            boolean bookRoom = Boolean.parseBoolean(params[i]);
            boolean Flight_message = false;
            boolean Car_message = false;
            boolean Hotel_message = false;
            for (String flight : flights) {
              String toSend = "reserveflight," + id + "," + customerID + "," + flight;
              Flight_message = sendAndRecv(toSend, outToFlightRM, inFromFlightRM);
//							Flight_message.concat("\n");
              Trace.info("flightmessage: " + Flight_message);
            }
            if (bookCar) {
              Trace.info("Booking car");
              String toSend = "reservecar," + id + "," + customerID + "," + location;
              Car_message = sendAndRecv(toSend, outToCarRM, inFromCarRM);
              Trace.info("Carmessage: " + Car_message);
            }
            if (bookRoom) {
              Trace.info("Booking room");
              String toSend = "reserveroom," + id + "," + customerID + "," + location;
              Hotel_message = sendAndRecv(toSend, outToHotelRM, inFromHotelRM);
              Trace.info("Carmessage: " + Hotel_message);
            }

            messageToClient = String.valueOf(Hotel_message && Car_message && Flight_message);
            //do some parsing then forward to right RM
            break;
          case 5:
            String temp_message_Flight_query = sendAndRecvStr(message, outToFlightRM,
                inFromFlightRM);
            String temp_message_Car_query = sendAndRecvStr(message, outToCarRM, inFromCarRM);
            String temp_message_Hotel_query = sendAndRecvStr(message, outToHotelRM, inFromHotelRM);
            messageToClient =
                "Flight information: " + temp_message_Flight_query + "Car information: "
                    + temp_message_Car_query + "Hotel Information: " + temp_message_Hotel_query;
            break;
          case 6:

            boolean bool_flight_deleted = sendAndRecv(message, outToFlightRM, inFromFlightRM);
            boolean bool_car_deleted = sendAndRecv(message, outToCarRM, inFromCarRM);
            boolean bool_hotel_deleted = sendAndRecv(message, outToHotelRM, inFromHotelRM);

            messageToClient = "" + (bool_flight_deleted && bool_car_deleted && bool_hotel_deleted);

        }

        System.out.println("message:" + message);
        String result = "Working!";
        Trace.info("Writing message to Client: " + messageToClient);
        outToClient.println(messageToClient);
        Trace.info("Wrote message to Client: " + messageToClient);
      }
      socket.close();
      /**
       **/
      // System.out.println("message:"+message);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int forwardRM(String methodName) {
    if (methodName.compareToIgnoreCase("newflight") == 0
        || methodName.compareToIgnoreCase("deleteflight") == 0
        || methodName.compareToIgnoreCase("queryflight") == 0
        || methodName.compareToIgnoreCase("queryflightprice") == 0
        || methodName.compareToIgnoreCase("reserveflight") == 0) {
      return 0;
    } else if (methodName.compareToIgnoreCase("newcar") == 0
        || methodName.compareToIgnoreCase("deletecar") == 0
        || methodName.compareToIgnoreCase("querycar") == 0
        || methodName.compareToIgnoreCase("querycarprice") == 0
        || methodName.compareToIgnoreCase("reservecar") == 0) {
      return 1;
    } else if (methodName.compareToIgnoreCase("newroom") == 0
        || methodName.compareToIgnoreCase("deleteroom") == 0
        || methodName.compareToIgnoreCase("queryroom") == 0
        || methodName.compareToIgnoreCase("queryroomprice") == 0
        || methodName.compareToIgnoreCase("reserveroom") == 0) {
      return 2;
    } else if (methodName.compareToIgnoreCase("newcustomerid") == 0
        || methodName.compareToIgnoreCase("newcustomer") == 0) {
      return 3;
    } else if (methodName.compareToIgnoreCase("itinerary") == 0) {
      return 4;
    } else if (methodName.compareToIgnoreCase("querycustomer") == 0) {
      return 5;
    } else if (methodName.compareToIgnoreCase("deletecustomer") == 0) {
      return 6;
    } else {
      return -1;
    }

  }

  public Boolean sendAndRecv(String msg, PrintWriter out, BufferedReader in) {
    out.println(msg);
    try {
      return Boolean.parseBoolean(in.readLine());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return false;
  }

  public int sendAndRecvInt(String msg, PrintWriter out, BufferedReader in) {
    out.println(msg);
    try {
      return Integer.parseInt(in.readLine());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

  public String sendAndRecvStr(String msg, PrintWriter out, BufferedReader in) {
    Trace.info(msg);
    out.println(msg);
    try {
      return in.readLine();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Trace.info("Returning null");
    return null;
  }

  public Boolean checkNumeric(String msg) {
    Trace.info("Hit");
    boolean toReturn;
    try {
      int x = (int) Integer.parseInt(msg);
      toReturn = true;
    } catch (Exception e) {
      toReturn = false;
    }
    return toReturn;
  }


}
