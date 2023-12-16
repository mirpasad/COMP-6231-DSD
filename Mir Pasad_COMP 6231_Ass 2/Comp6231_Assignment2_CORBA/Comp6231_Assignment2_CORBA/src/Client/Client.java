package Client;

import DataModel.MovieModel;
import Logger.Logger;
import ServerObjectInterfaceApp.ServerObjectInterface;
import ServerObjectInterfaceApp.ServerObjectInterfaceHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.util.Scanner;

public class Client {
    public static final int customerUser = 1;
    public static final int adminUser = 2;
    public static final int PurchaseMovieTicketCustomer = 1;
    public static final int SchedulingReservationCustomer = 2;
    public static final int CancelTicketCustomer = 3;
    public static final int SwapMovieCustomer = 4;
    public static final int LogoutCustomer = 5;
    public static final int InsertMovieAdmin = 1;
    public static final int RemoveMovieAdmin = 2;
    public static final int displayAvailabilityAdmin = 3;
    public static final int BookMovieAdmin = 4;
    public static final int ScheduleReservationAdmin = 5;
    public static final int CancelTicketAdmin = 6;
    public static final int SwapTicketAdmin = 7;
    public static final int LogoutAdmin = 8;
    public static final int Shutdown = 0;

    static Scanner input;

    public static void main(String[] args) throws Exception {
        try {
            ORB orb = ORB.init(args, null);
            // -ORBInitialPort 1050 -ORBInitialHost localhost
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            init(ncRef);
        } catch (Exception e) {
            System.out.println("Client ORB init exception: " + e);
            e.printStackTrace();
        }
    }

    public static void init(NamingContextExt ncRef) throws Exception {
        input = new Scanner(System.in);
        String userID;
        System.out.println("You must enter your UserID here:");
        userID = input.next().trim().toUpperCase();
            Logger.clientLog(userID, " Attempt to login");
            switch (checkUserType(userID)) {
                case customerUser:
                    try {
                        System.out.println("Customer Successfully Logged in(" + userID + ")");
                        Logger.clientLog(userID, " Customer Successfully Logged in");
                        customer(userID, ncRef);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case adminUser:
                    try {
                        System.out.println("Admin Successfully Logged in (" + userID + ")");
                        Logger.clientLog(userID, " Admin Successfully Logged in");
                        admin(userID, ncRef);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    System.out.println("!!The format of UserID is incorrect");
                    Logger.clientLog(userID, " The format of UserID is incorrect");
                    Logger.deleteALogFile(userID);
                    init(ncRef);
            }
    }


    private static String getServerID(String userID) {
        String branchAcronym = userID.substring(0, 3);
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            return branchAcronym;
        }
        return "1";
    }

    private static int checkUserType(String userID) {
        if (userID.length() == 8) {
            if (userID.substring(0, 3).equalsIgnoreCase("ATW") ||
                    userID.substring(0, 3).equalsIgnoreCase("VER") ||
                    userID.substring(0, 3).equalsIgnoreCase("OUT")) {
                if (userID.substring(3, 4).equalsIgnoreCase("C")) {
                    return customerUser;
                } else if (userID.substring(3, 4).equalsIgnoreCase("A")) {
                    return adminUser;
                }
            }
        }
        return 0;
    }

    private static void customer(String customerID, NamingContextExt ncRef) throws Exception {
        String serverID = getServerID(customerID);
        if (serverID.equals("1")) {
            init(ncRef);
        }
        ServerObjectInterface servant = ServerObjectInterfaceHelper.narrow(ncRef.resolve_str(serverID));
        boolean repeat = true;
        printMenu(customerUser);
        int menuSelection = input.nextInt();
        String movieName;
        String movieID;
        int numberOfTickets;
        String serverResponse;
        switch (menuSelection) {
            case PurchaseMovieTicketCustomer:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(customerID, " Movie Ticket Booking Process Begins");
                serverResponse = servant.reserveMovieTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " reserveMovieTickets", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SchedulingReservationCustomer:
                Logger.clientLog(customerID, " Get Reservation Schedule Process Begins");
                serverResponse = servant.obtainBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " reserveMovieTickets", " null ", serverResponse);
                break;
            case CancelTicketCustomer:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(customerID, " Cancelling Movie Tickets Process Begins");
                serverResponse = servant.cancelMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " reserveMovieTickets", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SwapMovieCustomer:
                System.out.println("Input the OLD Movie that will be replaced");
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                System.out.println("Input the NEW Movie that will be replaced");
                String newMovieName = promptForMovieName();
                String newMovieID = promptForMovieID();
                Logger.clientLog(customerID, " Swapping Movie Process Begins ");
                serverResponse = servant.swapMovie(customerID, newMovieID, newMovieName, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " swapMovie", " oldMovieID: " + movieID + " oldMovieName: " + movieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", serverResponse);
                break;
            case Shutdown:
                Logger.clientLog(customerID, " Shutting Down ORB");
                servant.shutdown();
                Logger.clientLog(customerID, " shutdown");
                return;
            case LogoutCustomer:
                repeat = false;
                Logger.clientLog(customerID, " attempting to Logout");
                init(ncRef);
                break;
        }
        if (repeat) {
            customer(customerID, ncRef);
        }
    }

    private static void admin(String movieAdminID, NamingContextExt ncRef) throws Exception {
        String serverID = getServerID(movieAdminID);
        if (serverID.equals("1")) {
            init(ncRef);
        }
        ServerObjectInterface servant = ServerObjectInterfaceHelper.narrow(ncRef.resolve_str(serverID));
        boolean repeat = true;
        printMenu(adminUser);
        String customerID;
        String movieName;
        String movieID;
        String serverResponse;
        int capacity;
        int numberOfTickets;
        int menuSelection = input.nextInt();
        switch (menuSelection) {
            case InsertMovieAdmin:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                capacity = promptForCapacity();
                Logger.clientLog(movieAdminID, " Insert Movie Slots Process Begins");
                serverResponse = servant.insertMovieSlots(movieID, movieName, capacity);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " insertMovieSlots", " movieID: " + movieID + " movieName: " + movieName + " movieCapacity: " + capacity + " ", serverResponse);
                break;
            case RemoveMovieAdmin:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " Process Start to RemoveMovieSlots");
                serverResponse = servant.removeMovieSlots(movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " removeMovieSlots", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case displayAvailabilityAdmin:
                movieName = promptForMovieName();
                Logger.clientLog(movieAdminID, " Display Movie Availability Process Begins");
                serverResponse = servant.displayAvailability(movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " displayAvailability", " movieName: " + movieName + " ", serverResponse);
                break;
            case BookMovieAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(movieAdminID, " Process to Reserve Movie Begins");
                serverResponse = servant.reserveMovieTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " reserveMovieTickets", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case ScheduleReservationAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                Logger.clientLog(movieAdminID, " Get Reservation Schedule Process Begins");
                serverResponse = servant.obtainBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " obtainBookingSchedule", " customerID: " + customerID + " ", serverResponse);
                break;
            case CancelTicketAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " Cancel Movie Tickets Process Begins");
                serverResponse = servant.cancelMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " cancelMovieTickets", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SwapTicketAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                System.out.println("Input the OLD Movie to be swapped");
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                System.out.println("Input the NEW Movie to be swapped");
                String newMovieName = promptForMovieName();
                String newMovieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " Swapping Movie Process Begins");
                serverResponse = servant.swapMovie(customerID, newMovieID, newMovieName, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " swapMovie", " customerID: " + customerID + " oldMovieID: " + movieID + " oldMovieName: " + movieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", serverResponse);
                break;
            case Shutdown:
                Logger.clientLog(movieAdminID, " Shutting Down ORB");
                servant.shutdown();
                Logger.clientLog(movieAdminID, " shutdown");
                return;
            case LogoutAdmin:
                repeat = false;
                Logger.clientLog(movieAdminID, "attempting to Logout");
                init(ncRef);
                break;
        }
        if (repeat) {
            admin(movieAdminID, ncRef);
        }
    }

    private static String RequestCustomerIDfromAdmin(String branchAcronym) {
        System.out.println("Please enter a customerID(Within " + branchAcronym + " Server):");
        String userID = input.next().trim().toUpperCase();
        if (checkUserType(userID) != customerUser || !userID.substring(0, 3).equals(branchAcronym)) {
            return RequestCustomerIDfromAdmin(branchAcronym);
        } else {
            return userID;
        }
    }

    private static void printMenu(int userType) {
        System.out.println("Please choose an option below:");
        if (userType == customerUser) {
            System.out.println("1.Reserve Movie Tickets");
            System.out.println("2.Look for Movie Reservation Schedule");
            System.out.println("3.Cancel Movie Tickets");
            System.out.println("4.Swap Movie Tickets");
            System.out.println("5.Logout");
            System.out.println("0.ShutDown");
        } else if (userType == adminUser) {
            System.out.println("1.Insert Movie");
            System.out.println("2.Remove Movie");
            System.out.println("3.Show list of Movie Available");
            System.out.println("4.Reserve Movie Tickets");
            System.out.println("5.Look for Movie Reservation Schedule");
            System.out.println("6.Cancel Movie Tickets");
            System.out.println("7.Swap Movie Tickets");
            System.out.println("8.Logout");
            System.out.println("0.ShutDown");
        }
    }

    private static String promptForMovieName() {
        System.out.println("Choose a movieName below:");
        System.out.println("1.Avatar");
        System.out.println("2.Avengers");
        System.out.println("3.Titanic");
        switch (input.nextInt()) {
            case 1:
                return MovieModel.Avatar;
            case 2:
                return MovieModel.Avengers;
            case 3:
                return MovieModel.Titanic;
        }
        return promptForMovieName();
    }

    private static String promptForMovieID() {
        System.out.println("Enter the MovieID (e.g ATWM190120)");
        String movieID = input.next().trim().toUpperCase();
        if (movieID.length() == 10) {
            if (movieID.substring(0, 3).equalsIgnoreCase("ATW") ||
                    movieID.substring(0, 3).equalsIgnoreCase("OUT") ||
                    movieID.substring(0, 3).equalsIgnoreCase("VER")) {
                if (movieID.substring(3, 4).equalsIgnoreCase("M") ||
                        movieID.substring(3, 4).equalsIgnoreCase("A") ||
                        movieID.substring(3, 4).equalsIgnoreCase("E")) {
                    return movieID;
                }
            }
        }
        return promptForMovieID();
    }

    private static int promptForNumberOfTickets() {
        System.out.println("Please enter the number of tickets: ");
        int numberOfTickets = Integer.parseInt(input.next().trim());
        return numberOfTickets;
    }
    private static int promptForCapacity() {
        System.out.println("Enter the booking capacity:");
        return input.nextInt();
    }
}
