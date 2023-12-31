package ServerObjectInterfaceApp;


/**
* ServerObjectInterfaceApp/ServerObjectInterfaceOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ./ServerObjectInterface.idl
* Saturday, 25 February, 2023 9:01:57 AM IST
*/

public interface ServerObjectInterfaceOperations 
{

  /**
        * Only admin
        */
  String insertMovieSlots (String movieID, String movieName, int bookingCapacity);
  String removeMovieSlots (String movieID, String movieName);
  String displayAvailability (String movieName);

  /**
        * Both admin and Customer
        */
  String reserveMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets);
  String obtainBookingSchedule (String customerID);
  String cancelMovieTickets (String customerID, String movieID, String movieName);
  String swapMovie (String customerID, String newMovieID, String newMovieName, String oldMovieID, String oldMovieName);
  void shutdown ();
} // interface ServerObjectInterfaceOperations
