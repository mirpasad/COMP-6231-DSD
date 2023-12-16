package com.web.client;

import DataModel.MovieModel;
import Logger.Logger;
import com.web.service.WebInterface;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
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

    public static Service atwaterService;
    public static Service outremontService;
    public static Service verdunService;
    private static WebInterface obj;

    static Scanner input;

    public static void main(String[] args) throws Exception {
        URL atwaterURL = new URL("http://localhost:8080/atwater?wsdl");
        QName atwaterQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
        atwaterService = Service.create(atwaterURL, atwaterQName);

        URL verdunURL = new URL("http://localhost:8080/verdun?wsdl");
        QName verdunQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
        verdunService = Service.create(verdunURL, verdunQName);

        URL outremontURL = new URL("http://localhost:8080/outremont?wsdl");
        QName outremontQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
        outremontService = Service.create(outremontURL, outremontQName);
        init();
    }

    public static void init() throws Exception {
        input = new Scanner(System.in);
        String userID;
        System.out.println("Please Enter your UserID(For Concurrency test enter 'ConTest'):");
        userID = input.next().trim().toUpperCase();
        if (userID.equalsIgnoreCase("ConTest")) {
            startConcurrencyTest();
        } else {
            Logger.clientLog(userID, " login attempt");
            switch (checkUserType(userID)) {
                case customerUser:
                    try {
                        System.out.println("Customer Login successful (" + userID + ")");
                        Logger.clientLog(userID, " Customer Login successful");
                        customer(userID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case adminUser:
                    try {
                        System.out.println("Admin Login successful (" + userID + ")");
                        Logger.clientLog(userID, " Admin Login successful");
                        manager(userID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    System.out.println("!!UserID is not in correct format");
                    Logger.clientLog(userID, " UserID is not in correct format");
                    Logger.deleteALogFile(userID);
                    init();
            }
        }
    }

    private static void startConcurrencyTest() throws Exception {
        System.out.println("Concurrency Test Starting for BookMovie");
        System.out.println("Connecting Atwater Server...");
        String movieName = MovieModel.AVATAR;
        String movieID = "ATWE101020";
        WebInterface servant = atwaterService.getPort(WebInterface.class);
        System.out.println("adding " + movieID + " " + movieName + " with capacity 2 to Atwater Server...");
        String response = servant.addMovie(movieID, movieName, 2);
        System.out.println(response);
        Runnable task1 = () -> {
            String customerID = "ATWC2345";
            String res = servant.bookMovie(customerID, movieID, movieName);
            System.out.println("Booking response for " + customerID + " " + res);
        };
        Runnable task2 = () -> {
            String customerID = "ATWC3456";
            String res = servant.bookMovie(customerID, movieID, movieName);
            System.out.println("Booking response for " + customerID + " " + res);
        };
        Runnable task3 = () -> {
            String customerID = "ATWC4567";
            String res = servant.bookMovie(customerID, movieID, movieName);
            System.out.println("Booking response for " + customerID + " " + res);
        };
        Runnable task4 = () -> {
            String res = servant.cancelMovie("ATWC2345", movieID, movieName);
            System.out.println("Canceling response for ATWC2345" + " " + res);

            res = servant.cancelMovie("ATWC3456", movieID, movieName);
            System.out.println("Canceling response for ATWC3456" + " " + res);

            res = servant.cancelMovie("ATWC4567", movieID, movieName);
            System.out.println("Canceling response for ATWC4567" + " " + res);
        };

        Runnable task5 = () -> {
            String res = servant.removeMovie(movieID, movieName);
            System.out.println("removeMovie response for " + movieID + " " + res);
        };

        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);
        Thread thread3 = new Thread(task3);
        Thread thread4 = new Thread(task4);
        Thread thread5 = new Thread(task5);

        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();

    
        thread4.start();
        thread4.join();

        System.out.println("Concurrency Test Finished for BookMovie");
        thread5.start();
        thread5.join();
    }

    private static String getServerID(String userID) {
        String branchAcronym = userID.substring(0, 3);
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            obj = atwaterService.getPort(WebInterface.class);
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            obj = outremontService.getPort(WebInterface.class);
            return branchAcronym;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            obj = verdunService.getPort(WebInterface.class);
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

    private static void customer(String customerID) throws Exception {
        String serverID = getServerID(customerID);
        if (serverID.equals("1")) {
            init();
        }
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
                Logger.clientLog(customerID, " attempting to bookMovie");
                serverResponse = obj.bookMovie(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " bookMovie", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SchedulingReservationCustomer:
                Logger.clientLog(customerID, " attempting to getBookingSchedule");
                serverResponse = obj.getBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " bookMovie", " null ", serverResponse);
                break;
            case CancelTicketCustomer:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(customerID, " attempting to cancelMovie");
                serverResponse = obj.cancelMovie(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " bookMovie", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SwapMovieCustomer:
                System.out.println("Please Enter the OLD movie to be replaced");
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                System.out.println("Please Enter the NEW movie to be replaced");
                String newMovieName = promptForMovieName();
                String newMovieID = promptForMovieID();
                Logger.clientLog(customerID, " attempting to swapMovie");
                serverResponse = obj.swapMovie(customerID, newMovieID, newMovieName, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " swapMovie", " oldMovieID: " + movieID + " oldMovieName: " + movieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", serverResponse);
                break;
            case LogoutCustomer:
                repeat = false;
                Logger.clientLog(customerID, " attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            customer(customerID);
        }
    }

    private static void manager(String movieAdminID) throws Exception {
        String serverID = getServerID(movieAdminID);
        if (serverID.equals("1")) {
            init();
        }
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
                Logger.clientLog(movieAdminID, " attempting to addMovie");
                serverResponse = obj.addMovie(movieID, movieName, capacity);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " addMovie", " movieID: " + movieID + " movieName: " + movieName + " movieCapacity: " + capacity + " ", serverResponse);
                break;
            case RemoveMovieAdmin:
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to removeMovie");
                serverResponse = obj.removeMovie(movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " removeMovie", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case displayAvailabilityAdmin:
                movieName = promptForMovieName();
                Logger.clientLog(movieAdminID, " attempting to listMovieAvailability");
                serverResponse = obj.listMovieAvailability(movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " listMovieAvailability", " movieName: " + movieName + " ", serverResponse);
                break;
            case BookMovieAdmin:
                customerID = askForCustomerIDFromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to bookMovie");
                serverResponse = obj.bookMovie(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " bookMovie", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case ScheduleReservationAdmin:
                customerID = askForCustomerIDFromAdmin(movieAdminID.substring(0, 3));
                Logger.clientLog(movieAdminID, " attempting to getBookingSchedule");
                serverResponse = obj.getBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " getBookingSchedule", " customerID: " + customerID + " ", serverResponse);
                break;
            case CancelTicketAdmin:
                customerID = askForCustomerIDFromAdmin(movieAdminID.substring(0, 3));
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to cancelMovie");
                serverResponse = obj.cancelMovie(customerID, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " cancelMovie", " customerID: " + customerID + " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case SwapTicketAdmin:
                customerID = askForCustomerIDFromAdmin(movieAdminID.substring(0, 3));
                System.out.println("Please Enter the OLD movie to be swapped");
                movieName = promptForMovieName();
                movieID = promptForMovieID();
                System.out.println("Please Enter the NEW movie to be swapped");
                String newMovieName = promptForMovieName();
                String newMovieID = promptForMovieID();
                Logger.clientLog(movieAdminID, " attempting to swapMovie");
                serverResponse = obj.swapMovie(customerID, newMovieID, newMovieName, movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminID, " swapMovie", " customerID: " + customerID + " oldMovieID: " + movieID + " oldMovieName: " + movieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", serverResponse);
                break;
            case LogoutAdmin:
                repeat = false;
                Logger.clientLog(movieAdminID, "attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            manager(movieAdminID);
        }
    }

    private static String askForCustomerIDFromAdmin(String branchAcronym) {
        System.out.println("Please enter a customerID(Within " + branchAcronym + " Server):");
        String userID = input.next().trim().toUpperCase();
        if (checkUserType(userID) != customerUser || !userID.substring(0, 3).equals(branchAcronym)) {
            return askForCustomerIDFromAdmin(branchAcronym);
        } else {
            return userID;
        }
    }

    private static void printMenu(int userType) {
     
        System.out.println("Please choose an option below:");
        if (userType == customerUser) {
            System.out.println("1.Book Movie");
            System.out.println("2.Get Booking Schedule");
            System.out.println("3.Cancel Movie");
            System.out.println("4.Swap Movie");
            System.out.println("5.Logout");
        } else if (userType == adminUser) {
            System.out.println("1.Add Movie");
            System.out.println("2.Remove Movie");
            System.out.println("3.List Movie Availability");
            System.out.println("4.Book Movie");
            System.out.println("5.Get Booking Schedule");
            System.out.println("6.Cancel Movie");
            System.out.println("7.Swap Movie");
            System.out.println("8.Logout");
        }
    }

    private static String promptForMovieName() {
        
        System.out.println("Please choose an movieName below:");
        System.out.println("1.Avatar");
        System.out.println("2.Avengers");
        System.out.println("3.Titanic");
        switch (input.nextInt()) {
            case 1:
                return MovieModel.AVATAR;
            case 2:
                return MovieModel.AVENGERS;
            case 3:
                return MovieModel.TITANIC;
        }
        return promptForMovieName();
    }

    private static String promptForMovieID() {
        
        System.out.println("Please enter the MovieID (e.g ATWM190120)");
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

    private static int promptForCapacity() {
        
        System.out.println("Please enter the booking capacity:");
        return input.nextInt();
    }
}
