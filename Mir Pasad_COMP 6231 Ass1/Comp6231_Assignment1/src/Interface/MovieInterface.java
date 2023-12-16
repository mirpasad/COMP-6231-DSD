package Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MovieInterface extends Remote {

    String insertMovieslots(String movieID, String movieName, int bookingCapacity) throws RemoteException;

    String removeMovieSlots(String movieID, String movieName) throws RemoteException;

    String displayAvailability(String movieName) throws RemoteException;

    String reserveMovieTickets(String customerID, String movieID, String movieName) throws RemoteException;

    String obtainBookingSchedule(String customerID) throws RemoteException;

    String cancelMovieTickets(String customerID, String movieID, String movieName) throws RemoteException;
}
