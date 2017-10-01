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


public class TCPMiddleWareThread implements Runnable{ 
	Socket socket;
	String carRMHostName;
	String hotelRMHostName;
	String flightRMHostName;
	int carRMPort;
	int flightRMPort; 
	int hotelRMPort;
	TCPMiddleWareThread (Socket socket, String aCarRMHostName, String aHotelRMHostName, String aFlightRMHostName, int aCarRMPort, int aFlightRMPort, int aHotelRMPort) {
		this.socket= socket;
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
			PrintWriter outToCarRM = new PrintWriter(carRMSocket.getOutputStream());
			BufferedReader inFromCarRM = new BufferedReader(new InputStreamReader(carRMSocket.getInputStream()));
			
			
			Socket hotelRMSocket = new Socket(hotelRMHostName, hotelRMPort);
			PrintWriter outToHotelRM = new PrintWriter(hotelRMSocket.getOutputStream());
			BufferedReader inFromHotelRM = new BufferedReader(new InputStreamReader(hotelRMSocket.getInputStream()));
			
			
			Socket flightRMSocket = new Socket(flightRMHostName, flightRMPort);
			PrintWriter outToFlightRM = new PrintWriter(flightRMSocket.getOutputStream());
			BufferedReader inFromFlightRM = new BufferedReader(new InputStreamReader(flightRMSocket.getInputStream()));
			
			
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			
		
			String message = null;
			String messageToClient = null;
				while ((message = inFromClient.readLine())!=null)
				{
					String[] params = message.split(",");
					String id = params[1];
					switch(forwardRM(params[0])) {
					case 0: 
						messageToClient = sendAndRecvStr(message,outToFlightRM, inFromFlightRM);
						//forward to Flight RM 
						break;
					case 1:
						messageToClient = sendAndRecvStr(message,outToCarRM, inFromCarRM);
						//forward to CarRM
						break;
					case 2:
						messageToClient = sendAndRecvStr(message,outToHotelRM, inFromHotelRM);
						//forward to HotelRM
						break;
					case 3:
						if (params[0].compareToIgnoreCase("newcustomer")==0){
							int cid = Integer.parseInt( String.valueOf(id) +
	                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
	                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
							message = "newcustomerid," + params[1]+ "," + cid; 
						}
						
						String temp_message_Flight = sendAndRecvStr(message,outToFlightRM, inFromFlightRM);
						String temp_message_Car = sendAndRecvStr(message, outToCarRM, inFromCarRM);
						String temp_message_Hotel = sendAndRecvStr(message, outToHotelRM, inFromHotelRM);
						messageToClient = temp_message_Flight + "\n" +  temp_message_Car + "\n" + temp_message_Hotel;
						//forward to FlightRM
						//forward to CarRM
						//forward to HotelRM
						break;
					case 4: 
						
						String customerID = params[2]; 
						Vector<String> flights = null;
						int i = 3; 
						while(checkNumeric(params[i])) {
							flights.add(params[i]);
							i++;
						}
						String location = params[i];
						i++; 
						Boolean bookCar = Boolean.parseBoolean(params[i]);
						i++;
						Boolean bookRoom = Boolean.parseBoolean(params[i]);
						String Flight_message="";
						String Car_message = "";
						String Hotel_message = "";
						for (String flight : flights) {
							String toSend = "reserveflight,"+id + "," + customerID + "," + flight;
							Flight_message.concat(sendAndRecvStr(toSend, outToFlightRM, inFromFlightRM));
							Flight_message.concat("\n");
						}
						if (bookCar){
							
							String toSend = "reservecar," + id + "," + customerID + "," +location;
							Car_message = sendAndRecvStr(toSend, outToCarRM, inFromCarRM);
						}
						if (bookRoom){
							String toSend = "reserveroom," + id + "," + customerID + "," + location; 
							Hotel_message = sendAndRecvStr(toSend, outToHotelRM, inFromHotelRM);
						}
						
						messageToClient = Flight_message + "\n Car booking: \n " + Car_message + "\n Hotel booking: \n " + Hotel_message;
					
						
						//do some parsing then forward to right RM
					}
					
					
					
					

			System.out.println("message:"+message);
			String result="Working!";
				}
				outToClient.println(messageToClient);
				socket.close();
			/**
			**/
			// System.out.println("message:"+message);
			
			
		} catch (IOException e){
			
		}
	}
	
	public int forwardRM(String methodName){
		if (methodName.compareToIgnoreCase("newflight") == 0 || methodName.compareToIgnoreCase("deleteflight") == 0 || methodName.compareToIgnoreCase("queryflight") == 0 || methodName.compareToIgnoreCase("queryflightprice") == 0 || methodName.compareToIgnoreCase("reserveflight") == 0) {
			return 0;
		} else if (methodName.compareToIgnoreCase("newcar") == 0 || methodName.compareToIgnoreCase("deletecar") == 0 || methodName.compareToIgnoreCase("querycar") == 0 || methodName.compareToIgnoreCase("querycarprice") == 0 || methodName.compareToIgnoreCase("reservecar") == 0) {
			return 1;
		} else if (methodName.compareToIgnoreCase("newroom") == 0 || methodName.compareToIgnoreCase("deleteroom") == 0 || methodName.compareToIgnoreCase("queryroom") == 0 || methodName.compareToIgnoreCase("queryroomprice") == 0 || methodName.compareToIgnoreCase("reserveroom") == 0) {
			return 2;
		}  else if (methodName.compareToIgnoreCase("newcustomerid")==0 || methodName.compareToIgnoreCase("newcustomer") == 0 || methodName.compareToIgnoreCase("deletecustomer") == 0 || methodName.compareToIgnoreCase("querycustomer") == 0){
			return 3;
		}  else if (methodName.compareToIgnoreCase("itinerary")==0) {
			return 4;
		} else {
			return -1;
		}

	}
	
	  public Boolean sendAndRecv(String msg, PrintWriter out, BufferedReader in) {
		    out.println(msg);
		    try {
		      return Boolean.getBoolean(in.readLine());
		    } catch (IOException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		    }
		    return false;
		  }

		  public int sendAndRecvInt(String msg, PrintWriter out, BufferedReader in) {
		    out.println(msg);
		    try {
		      return Integer.getInteger(in.readLine());
		    } catch (IOException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		    }
		    return -1;
		  }

		  public String sendAndRecvStr(String msg, PrintWriter out, BufferedReader in) {
		    out.println(msg);
		    try {
		      return in.readLine();
		    } catch (IOException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		    }
		    return null;
		  }
		  
	public Boolean checkNumeric(String msg) {
		boolean toReturn;
		try {
			int x = (int) Integer.parseInt(msg);
			toReturn = true;
		} catch (Exception e){
			toReturn = false;
		}
		return toReturn;
	}
	
	
}