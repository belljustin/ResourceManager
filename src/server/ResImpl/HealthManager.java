package server.ResImpl;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import server.ResInterface.ICarManager;
import server.ResInterface.IFlightManager;
import server.ResInterface.IHotelManager;
import server.ResInterface.IResourceManager;

public class HealthManager extends Thread {
  MiddleWare mw;

  public HealthManager(MiddleWare mw) {
    this.mw = mw;
  }

  public void run() {
    while (true) {
      try {
        Thread.sleep(1000);

        try {
          mw.flightRM.ping();
        } catch (RemoteException e) {
          reconnectFlight();
        }

        try {
          mw.hotelRM.ping();
        } catch (RemoteException e) {
          reconnectHotel();
        }

        try {
          mw.carRM.ping();
        } catch (RemoteException e) {
          reconnectCar();
        }

      } catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(-1);
      }

      IResourceManager[] cohorts = {mw, mw.flightRM, mw.hotelRM, mw.carRM};
      mw.coordinator.updateCohort(cohorts);
    }
  }

  public void reconnectFlight() throws InterruptedException {
    Trace.error("Lost connection to flightRM");
    while (true) {
      try {
        mw.flightRM = (IFlightManager) mw.registry.lookup("PG12FlightRM");
        mw.flightRM.ping();
        break;
      } catch (RemoteException|NotBoundException e) {
        Trace.error("Still can't connect to FlightRM");
        Thread.sleep(1000);
      }
    }
    Trace.info("Successfully reconnected to flightRM");
  }

  public void reconnectHotel() throws InterruptedException {
    Trace.error("Lost connection to hotelRM");
    while (true) {
      try {
        mw.hotelRM = (IHotelManager) mw.registry.lookup("PG12HotelRM");
        mw.hotelRM.ping();
        break;
      } catch (RemoteException|NotBoundException e) {
        Trace.error("Still can't connect to HotelRM");
        Thread.sleep(1000);
      }
    }
    Trace.info("Successfully reconnected to HotelRM");
  }

  public void reconnectCar() throws InterruptedException {
    Trace.error("Lost connection to carRM");
    while (true) {
      try {
        mw.carRM = (ICarManager) mw.registry.lookup("PG12CarRM");
        mw.carRM.ping();
        break;
      } catch (RemoteException|NotBoundException e) {
        Trace.error("Still can't connect to CarRM");
        Thread.sleep(1000);
      }
    }
    Trace.info("Successfully reconnected to CarRM");
  }
}
