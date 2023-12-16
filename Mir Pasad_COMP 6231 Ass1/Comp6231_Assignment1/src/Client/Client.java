package Client;

import DataModel.MovieModel;
import Interface.MovieInterface;
import Logger.Logger;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

//Variable Declaration
public class Client {
    public static final int customerUser = 1;
    public static final int adminUser = 2;
    public static final int PurchaseMovieTicketCustomer = 1;
    public static final int SchedulingReservationCustomer = 2;
    public static final int CancelTicketCustomer = 3;
    public static final int LogoutCustomer = 4;
    public static final int InsertMovieAdmin = 1;
    public static final int RemoveMovieAdmin = 2;
    public static final int DisplayAvailabilityAdmin = 3;
    public static final int BookMovieAdmin = 4;
    public static final int ScheduleReservationAdmin = 5;
    public static final int CancelTicketAdmin = 6;
    public static final int LogoutAdmin = 7;
    public static final int ServerATW = 1111;
    public static final int ServerVER = 2222;
    public static final int ServerOUT = 3333;
    public static final String registeredName = "MOVIE BOOKING ";

    static Scanner input;

    public static void main(String[] args) throws Exception {
        Login();
    }

    //Login Function that allows the Customer and Admin to login and and verify Whether a customer has logged in or admin
    //Also checks invalid username format
    public static void Login() throws IOException {
        input = new Scanner(System.in);
        String userID;
        System.out.println("You must enter your UserID here:");
        userID = input.next().trim().toUpperCase();
        Logger.clientLog(userID, " Attempt to login");
        switch (getUserType(userID)) {
            case customerUser:
                try {
                    System.out.println("Customer Successfully Logged in (" + userID + ")");
                    Logger.clientLog(userID, " Customer Successfully Logged in");
                    customer(userID, getServerPort(userID.substring(0, 3)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case adminUser:
                try {
                    System.out.println("Admin Successfully Logged in (" + userID + ")");
                    Logger.clientLog(userID, " Admin Successfully Logged in");
                    admin(userID, getServerPort(userID.substring(0, 3)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("!!The format of UserID is incorrect");
                Logger.clientLog(userID, " The format of UserID is incorrect");
                Logger.deleteALogFile(userID);
                Login();
        }
    }

    //Selects the port number for particular server
    private static int getServerPort(String branch) {
        if (branch.equalsIgnoreCase("ATW")) {
            return ServerATW;
        } else if (branch.equalsIgnoreCase("VER")) {
            return ServerVER;
        } else if (branch.equalsIgnoreCase("OUT")) {
            return ServerOUT;
        }
        return 1;
    }

    //Here we are Declaring the format for the userID for customer and Admin
    //length upto 8 character and 'C' & 'A' determines customer and Admin
    private static int getUserType(String userID) {
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

    // Adding what all functionalities Customer can perform
    //Purchase Tickets, See what booking he has made, Cancel Tickets and logout
    private static void customer(String customerID, int serverPort) throws Exception {
        if (serverPort == 1) {
            return;
        }
        Registry registry = LocateRegistry.getRegistry(serverPort);
        MovieInterface remoteObject = (MovieInterface) registry.lookup(registeredName);
        boolean repeat = true;
        printMenu(customerUser);
        int menuSelection = input.nextInt();
        String movieName;
        String movieID;
        String serverResponse;
        switch (menuSelection) {
            case PurchaseMovieTicketCustomer:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(customerID, " Movie Ticket Booking Process Begins");
                serverResponse = remoteObject.reserveMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " PurchaseMovieTickets", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SchedulingReservationCustomer:
                Logger.clientLog(customerID, " Get Booking Schedule Process Begins");
                serverResponse = remoteObject.obtainBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " SchedulingReservation", " null ", serverResponse);
                break;
            case CancelTicketCustomer:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(customerID, " Cancel Movie Tickets Process Begins");
                serverResponse = remoteObject.cancelMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " CancelMovieTickets", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case LogoutCustomer:
                repeat = false;
                Logger.clientLog(customerID, " Attempting to Logout");
                Login();
                break;
        }
        if (repeat) {
            customer(customerID, serverPort);
        }
    }

    //Adding functionalities Admin can perform
    // Insert and Remove movie Shows, Availability of movie and logout
    //It can also reserve or cancel movie tickets on behalf of customer
    private static void admin(String movieAdminID, int serverPort) throws Exception {
        if (serverPort == 1) {
            return;
        }
        Registry registry = LocateRegistry.getRegistry(serverPort);
        MovieInterface remoteObject = (MovieInterface) registry.lookup(registeredName);
        boolean repeat = true;
        printMenu(adminUser);
        String customerID;
        String movieName;
        String movieID;
        String serverResponse;
        int capacity;
        int menuSelection = input.nextInt();
        switch (menuSelection) {
            case InsertMovieAdmin:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                capacity = promptForCapacity();
                Logger.clientLog(movieAdminID, " Insert Movie Slots Process Begins");
                serverResponse = remoteObject.insertMovieslots(movieID, movieName, capacity);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " InsertMovieSlots", " movieID: " + movieID + " movieName: " + movieName + " movieCapacity: " + capacity + " ", serverResponse);
                break;
            case RemoveMovieAdmin:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " Process Start to RemoveMovieSlots");
                serverResponse = remoteObject.removeMovieSlots(movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " RemoveMovieSlots", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case DisplayAvailabilityAdmin:
                movieName = promptForMovieName();
                Logger.clientLog(movieAdminID, " Display Movie Availability Process Begins");
                serverResponse = remoteObject.displayAvailability(movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " DisplayAvailability", " movieName: " + movieName + " ", serverResponse);
                break;
            case BookMovieAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to reserveMovieTickets");
                serverResponse = remoteObject.reserveMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " reserveMovieTickets", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case ScheduleReservationAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                Logger.clientLog(movieAdminID, " attempting to ScheduleReservation");
                serverResponse = remoteObject.obtainBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " ScheduleReservation", " customerID: " + customerID + " ", serverResponse);
                break;
            case CancelTicketAdmin:
                customerID = RequestCustomerIDfromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to CancelTicket");
                serverResponse = remoteObject.cancelMovieTickets(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " CancelTicket", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case LogoutAdmin:
                repeat = false;
                Logger.clientLog(movieAdminID, "Attempting to Logout");
                Login();
                break;
        }
        if (repeat) {
            admin(movieAdminID, serverPort);
        }
    }

    private static String RequestCustomerIDfromAdmin(String branchAcronym) {
        System.out.println("Please enter a customerID(Within " + branchAcronym + " Server):");
        String userID = input.next().trim().toUpperCase();
        if (getUserType(userID) != customerUser || !userID.substring(0, 3).equals(branchAcronym)) {
            return RequestCustomerIDfromAdmin(branchAcronym);
        } else {
            return userID;
        }
    }

    //Creating Menu of functionalities Customer and Admin can perform
    private static void printMenu(int userType) {
        System.out.println("Choose an option below:");
        if (userType == customerUser) {
            System.out.println("1.Reserve Movie Tickets");
            System.out.println("2.Look for Movie Reservation Schedule");
            System.out.println("3.Cancel Movie Tickets");
            System.out.println("4.Logout");
        } else if (userType == adminUser) {
            System.out.println("1.Insert Movie");
            System.out.println("2.Remove Movie");
            System.out.println("3.Show list of Movie Available");
            System.out.println("4.Reserve Movie Tickets");
            System.out.println("5.Look for Movie Reservation Schedule");
            System.out.println("6.Cancel Movie Tickets");
            System.out.println("7.Logout");
        }
    }

    //Option to book tickets for three movies
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

    //Option to book movie at which movie theatre and at which time
    //Station- ATWATER, VERDUN, OUTREMONT and Timings- Morning, Afternoon, Evening
    private static String promptForMovieID() {
        System.out.println("Enter the MovieID (e.g ATWM1111111)");
        String movieID = input.next().trim().toUpperCase();
        if (movieID.length() == 10) {
            if (movieID.substring(0, 3).equalsIgnoreCase("ATW") ||
                    movieID.substring(0, 3).equalsIgnoreCase("VER") ||
                    movieID.substring(0, 3).equalsIgnoreCase("OUT")) {
                if (movieID.substring(3, 4).equalsIgnoreCase("M") ||
                        movieID.substring(3, 4).equalsIgnoreCase("A") ||
                        movieID.substring(3, 4).equalsIgnoreCase("E")) {
                    return movieID;
                }
            }
        }
        return promptForMovieID();
    }

    //Ask for the booking capacity to Admin while adding the movie.
    private static int promptForCapacity() {
        System.out.println("Enter the booking capacity:");
        return input.nextInt();
    }
}
