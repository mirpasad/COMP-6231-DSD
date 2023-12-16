package ServerInterface;

import DataModel.ClientModel;
import DataModel.MovieModel;
import Logger.Logger;
import ServerObjectInterfaceApp.ServerObjectInterfacePOA;
import org.omg.CORBA.ORB;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MovieM extends ServerObjectInterfacePOA {
    private ORB orb;
    public static final int ATWServerPort = 1322;
    public static final int VERServerPort = 2213;
    public static final int OUTServerPort = 2132;
    public static final int MINVALUE = Integer.MIN_VALUE;
    public static final String OUTMovieServer = "Outremont";
    public static final String VERMovieServer = "Verdun";
    public static final String ATWMovieServer = "Atwater";
    private String serverID;
    private String serverName;
    private Map<String, Map<String, MovieModel>> allMovies;
    private Map<String, Map<String, List<String>>> clientMovies;
    private Map<String, ClientModel> serverClients;
    private Map<String, Integer> movieBookings;

    public MovieM(String serverID, String serverName) {
        super();
        this.serverID = serverID;
        this.serverName = serverName;
        allMovies = new ConcurrentHashMap<>();
        allMovies.put(MovieModel.Avatar, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.Avengers, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.Titanic, new ConcurrentHashMap<>());
        clientMovies = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();
        movieBookings = new ConcurrentHashMap<>();
    }

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }


    private static int getServerPort(String branchAcronym) {
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            return ATWServerPort;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            return OUTServerPort;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            return VERServerPort;
        }
        return 1;
    }

    @Override
    public String insertMovieSlots(String movieID, String movieName, int bookingCapacity) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (movieExists(movieName, movieID)) {
                if (allMovies.get(movieName).get(movieID).getMovieCapacity() <= bookingCapacity) {
                    allMovies.get(movieName).get(movieID).setMovieCapacity(bookingCapacity);
                    response = "Success: Movie " + movieID + " Capacity increased to " + bookingCapacity;
                    try {
                        Logger.serverLog(serverID, "null", " CORBA insertMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    response = "Failure: Cannot Lower Booking Capacity Due to Existence of the Movie";
                    try {
                        Logger.serverLog(serverID, "null", " CORBA insertMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }
            } else {
                MovieModel movie = new MovieModel(movieName, movieID, bookingCapacity);
                Map<String, MovieModel> movieHashMap = allMovies.get(movieName);
                movieHashMap.put(movieID, movie);
                allMovies.put(movieName, movieHashMap);
                response = "Success: Movie " + movieID + " added successfully";
                try {
                    Logger.serverLog(serverID, "null", " CORBA insertMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            response = "Failed: Unable to insert movie to servers besides" + serverName;
            try {
                Logger.serverLog(serverID, "null", " CORBA insertMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    @Override
    public String removeMovieSlots(String movieID, String movieName) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (movieExists(movieName, movieID)) {
                List<String> registeredClients = allMovies.get(movieName).get(movieID).getRegisteredClientIDs();
                allMovies.get(movieName).remove(movieID);
                insertCustomersToNextSameMovie(movieID, movieName, registeredClients);
                response = "Success: Movie " + movieID + " Removed Successfully";
                try {
                    Logger.serverLog(serverID, "null", " CORBA removeMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " Does Not Exist";
                try {
                    Logger.serverLog(serverID, "null", " CORBA removeMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            response = "Failed: Cannot Remove Movie from servers other than " + serverName;
            try {
                Logger.serverLog(serverID, "null", " CORBA removeMovieSlots ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    @Override
    public String displayAvailability(String movieName) {
        String response;
        Map<String, MovieModel> movies = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ":\n");
        if (movies.size() == 0) {
            builder.append("No Movies of Type " + movieName + "\n");
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movie.toString() + " || ");
            }
        }
        String otherServer1, otherServer2;
        if (serverID.equals("ATW")) {
            otherServer1 = sendUDPMessage(OUTServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
            otherServer2 = sendUDPMessage(VERServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
        } else if (serverID.equals("OUT")) {
            otherServer1 = sendUDPMessage(VERServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
            otherServer2 = sendUDPMessage(ATWServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
        } else {
            otherServer1 = sendUDPMessage(ATWServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
            otherServer2 = sendUDPMessage(OUTServerPort, "displayAvailability", "null", movieName, "null", MINVALUE);
        }
        builder.append(otherServer1).append(otherServer2);
        response = builder.toString();
        try {
            Logger.serverLog(serverID, "null", " CORBA displayAvailability ", " movieName: " + movieName + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String reserveMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets) {
        String response;
        checkClientExists(customerID);
        if (isMovieOfThisServer(movieID)) {
            MovieModel bookedMovie = allMovies.get(movieName).get(movieID);
            if (bookedMovie == null) {
                response = "Failed: Movie " + movieID + " Does not exists";
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
            if (!bookedMovie.isFull() && numberOfTickets <= bookedMovie.getMovieRemainCapacity()) {
                if (clientMovies.containsKey(customerID)) {
                    if (clientMovies.get(customerID).containsKey(movieName)) {
                        if (!clientHasMovie(customerID, movieName, movieID)) {
                            if (isCustomerOfThisServer(customerID))
                                clientMovies.get(customerID).get(movieName).add(movieID);
                        } else {
                            response = "Failed: Movie " + movieID + " Already Booked";
                            try {
                                Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                    } else {
                        if (isCustomerOfThisServer(customerID))
                            insertMovieSlotsTypeAndMovie(customerID, movieName, movieID);
                    }
                } else {
                    if (isCustomerOfThisServer(customerID))
                        insertCustomerAndMovie(customerID, movieName, movieID);
                }
                if (allMovies.get(movieName).get(movieID).InsertRegisteredClientID(customerID) == MovieModel.ADD_SUCCESS) {
                    response = "Success: Movie " + movieID + " Booked Successfully "+ numberOfTickets + " Tickets";
                } else if (allMovies.get(movieName).get(movieID).InsertRegisteredClientID(customerID) == MovieModel.TicketsFull) {
                    response = "Failed: Movie " + movieID + " is Full "+ numberOfTickets + " Tickets available!";
                } else {
                    response = "Failed: Cannot Insert You To Movie " + movieID;
                }
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " is Full " + numberOfTickets + " Tickets available!";
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            movieBookings.put(customerID + movieID + movieName, numberOfTickets);
            System.out.println(movieBookings);
            return response;
        } else {
            if (clientHasMovie(customerID, movieName, movieID)) {
                String serverResponse = "Failed: Movie " + movieID + " Already Booked";
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return serverResponse;
            }
            if (exceedWeeklyLimit(customerID, movieID.substring(4))) {
                String serverResponse = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "reserveMovieTickets", customerID, movieName, movieID, numberOfTickets);
                if (serverResponse.startsWith("Success:")) {
                    if (clientMovies.get(customerID).containsKey(movieName)) {
                        clientMovies.get(customerID).get(movieName).add(movieID);
                    } else {
                        List<String> temp = new ArrayList<>();
                        temp.add(movieID);
                        clientMovies.get(customerID).put(movieName, temp);
                    }
                }
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                movieBookings.put(customerID + movieID + movieName, numberOfTickets);
                System.out.println(movieBookings);
                return serverResponse;
            } else {
                response = "Failed: You Cannot Book Movie in Other Servers For This Week(Max Weekly Limit = 3)";
                try {
                    Logger.serverLog(serverID, customerID, " CORBA reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    @Override
    public String obtainBookingSchedule(String customerID) {
        String response;
        if (!checkClientExists(customerID)) {
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " CORBA obtainBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        Map<String, List<String>> movies = clientMovies.get(customerID);
        if (movies.size() == 0) {
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " CORBA obtainBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        StringBuilder builder = new StringBuilder();
        for (String movieName :
                movies.keySet()) {
            builder.append(movieName + ":\n");
            for (String movieID :
                    movies.get(movieName)) {
                builder.append(movieID + " ||");
            }
        }
        response = builder.toString();
        try {
            Logger.serverLog(serverID, customerID, " CORBA obtainBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String cancelMovieTickets(String customerID, String movieID, String movieName) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (isCustomerOfThisServer(customerID)) {
                if (!checkClientExists(customerID)) {
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    if (removeMovieSlotsIfExists(customerID, movieName, movieID)) {
                        allMovies.get(movieName).get(movieID).removeRegisteredClientID(customerID);
                        response = "Success: Movie " + movieID + " Canceled for " + customerID;
                        try {
                            Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    } else {
                        response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                        try {
                            Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (allMovies.get(movieName).get(movieID).removeRegisteredClientID(customerID)) {
                    response = "Success: Movie " + movieID + " Canceled for " + customerID;
                    try {
                        Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (isCustomerOfThisServer(customerID)) {
                if (checkClientExists(customerID)) {
                    if (removeMovieSlotsIfExists(customerID, movieName, movieID)) {
                        response = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "cancelMovieTickets", customerID, movieName, movieID,0);
                        try {
                            Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
            try {
                Logger.serverLog(serverID, customerID, " CORBA cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        return response;
    }

    @Override
    public String swapMovie(String customerID, String newMovieID, String newMovieName, String oldMovieID, String oldMovieName) {
        String response;
        if (!checkClientExists(customerID)) {
            response = "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
            try {
                Logger.serverLog(serverID, customerID, " CORBA swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        } else {
            if (clientHasMovie(customerID, oldMovieName, oldMovieID)) {
                String bookResp = "Failed: did not send book request for your newMovie " + newMovieID;
                String cancelResp = "Failed: did not send cancel request for your oldMovie " + oldMovieID;
                synchronized (this) {
                    if (onTheSameWeek(newMovieID.substring(4), oldMovieID) && !exceedWeeklyLimit(customerID, newMovieID.substring(4))) {
                        cancelResp = cancelMovieTickets(customerID, oldMovieID, oldMovieName);
                        if (cancelResp.startsWith("Success:")) {
                            bookResp = reserveMovieTickets(customerID, newMovieID, newMovieName,1);
                        }
                    } else {
                        bookResp = reserveMovieTickets(customerID, newMovieID, newMovieName, 1);
                        if (bookResp.startsWith("Success:")) {
                            cancelResp = cancelMovieTickets(customerID, oldMovieID, oldMovieName);
                        }
                    }
                }
                if (bookResp.startsWith("Success:") && cancelResp.startsWith("Success:")) {
                    response = "Success: Movie " + oldMovieID + " swapped with " + newMovieID;
                } else if (bookResp.startsWith("Success:") && cancelResp.startsWith("Failed:")) {
                    cancelMovieTickets(customerID, newMovieID, newMovieName);
                    response = "Failed: Your oldMovie " + oldMovieID + " Could not be Canceled reason: " + cancelResp;
                } else if (bookResp.startsWith("Failed:") && cancelResp.startsWith("Success:")) {
                    //hope this won't happen, but just in case.
                    String resp1 = reserveMovieTickets(customerID, oldMovieID, oldMovieName,1);
                    response = "Failed: Your newMovie " + newMovieID + " Could not be Booked reason: " + bookResp + " And your old movie Rolling back: " + resp1;
                } else {
                    response = "Failed: on Both newMovie " + newMovieID + " Booking reason: " + bookResp + " and oldMovie " + oldMovieID + " Canceling reason: " + cancelResp;
                }
                try {
                    Logger.serverLog(serverID, customerID, " CORBA swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
                try {
                    Logger.serverLog(serverID, customerID, " CORBA swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    @Override
    public void shutdown() {
        orb.shutdown(false);
    }

    public String removeMovieSlotsSlotsUDP(String oldMovieID, String movieName, String customerID) {
        if (!checkClientExists(customerID)) {
            return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
        } else {
            if (removeMovieSlotsIfExists(customerID, movieName, oldMovieID)) {
                return "Success: Movie " + oldMovieID + " Was Removed from " + customerID + " Schedule";
            } else {
                return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
            }
        }
    }

    public String displayAvailabilityUDP(String movieName) {
        Map<String, MovieModel> movies = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ":\n");
        if (movies.size() == 0) {
            builder.append("No Movies of Type " + movieName);
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movie.toString() + " || ");
            }
        }
        return builder.toString();
    }

    private String sendUDPMessage(int serverPort, String method, String customerID, String movieName, String movieId, Integer value) {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + customerID + ";" + movieName + ";" + movieId;
        try {
            Logger.serverLog(serverID, customerID, " UDP request sent " + method + " ", " movieID: " + movieId + " movieName: " + movieName + " ", " ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        try {
            Logger.serverLog(serverID, customerID, " UDP reply received" + method + " ", " movieID: " + movieId + " movieName: " + movieName + " ", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    private String getNextSameMovie(Set<String> keySet, String movieName, String oldMovieID) {
        List<String> sortedIDs = new ArrayList<String>(keySet);
        sortedIDs.add(oldMovieID);
        Collections.sort(sortedIDs, new Comparator<String>() {
            @Override
            public int compare(String ID1, String ID2) {
                Integer timeSlot1 = 0;
                switch (ID1.substring(3, 4).toUpperCase()) {
                    case "M":
                        timeSlot1 = 1;
                        break;
                    case "A":
                        timeSlot1 = 2;
                        break;
                    case "E":
                        timeSlot1 = 3;
                        break;
                }
                Integer timeSlot2 = 0;
                switch (ID2.substring(3, 4).toUpperCase()) {
                    case "M":
                        timeSlot2 = 1;
                        break;
                    case "A":
                        timeSlot2 = 2;
                        break;
                    case "E":
                        timeSlot2 = 3;
                        break;
                }
                Integer date1 = Integer.parseInt(ID1.substring(8, 10) + ID1.substring(6, 8) + ID1.substring(4, 6));
                Integer date2 = Integer.parseInt(ID2.substring(8, 10) + ID2.substring(6, 8) + ID2.substring(4, 6));
                int dateCompare = date1.compareTo(date2);
                int timeSlotCompare = timeSlot1.compareTo(timeSlot2);
                if (dateCompare == 0) {
                    return ((timeSlotCompare == 0) ? dateCompare : timeSlotCompare);
                } else {
                    return dateCompare;
                }
            }
        });
        int index = sortedIDs.indexOf(oldMovieID) + 1;
        for (int i = index; i < sortedIDs.size(); i++) {
            if (!allMovies.get(movieName).get(sortedIDs.get(i)).isFull()) {
                return sortedIDs.get(i);
            }
        }
        return "Failed";
    }

    private boolean exceedWeeklyLimit(String customerID, String movieDate) {
        int limit = 0;
        for (int i = 0; i < 3; i++) {
            List<String> registeredIDs = new ArrayList<>();
            switch (i) {
                case 0:
                    if (clientMovies.get(customerID).containsKey(MovieModel.Avatar)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.Avatar);
                    }
                    break;
                case 1:
                    if (clientMovies.get(customerID).containsKey(MovieModel.Avengers)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.Avengers);
                    }
                    break;
                case 2:
                    if (clientMovies.get(customerID).containsKey(MovieModel.Titanic)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.Titanic);
                    }
                    break;
            }
            for (String movieID :
                    registeredIDs) {
                if (onTheSameWeek(movieDate, movieID) && !isMovieOfThisServer(movieID)) {
                    limit++;
                }
                if (limit == 3)
                    return false;
            }
        }
        return true;
    }

    private void insertCustomersToNextSameMovie(String oldMovieID, String movieName, List<String> registeredClients) {
        String response;
        for (String customerID :
                registeredClients) {
            if (customerID.substring(0, 3).equals(serverID)) {
                removeMovieSlotsIfExists(customerID, movieName, oldMovieID);
                String nextSameMovieResult = getNextSameMovie(allMovies.get(movieName).keySet(), movieName, oldMovieID);
                if (nextSameMovieResult.equals("Failed")) {
                    response = "Acquiring nextSameMovie :" + nextSameMovieResult;
                    try {
                        Logger.serverLog(serverID, customerID, " insertCustomersToNextSameMovie ", " oldMovieID: " + oldMovieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                } else {
                    reserveMovieTickets(customerID, nextSameMovieResult, movieName, 0);
                }
            } else {
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeMovieSlots", customerID, movieName, oldMovieID, MINVALUE);
            }
        }
    }

    private synchronized boolean movieExists(String movieName, String movieID) {
        return allMovies.get(movieName).containsKey(movieID);
    }

    private synchronized boolean isMovieOfThisServer(String movieID) {
        return MovieModel.detectMovieServer(movieID).equals(serverName);
    }

    private synchronized boolean checkClientExists(String customerID) {
        if (!serverClients.containsKey(customerID)) {
            insertNewCustomerToClients(customerID);
            return false;
        } else {
            return true;
        }
    }

    private synchronized boolean clientHasMovie(String customerID, String movieName, String movieID) {
        if (clientMovies.get(customerID).containsKey(movieName)) {
            return clientMovies.get(customerID).get(movieName).contains(movieID);
        } else {
            return false;
        }
    }

    private boolean removeMovieSlotsIfExists(String customerID, String movieName, String movieID) {
        if (clientMovies.get(customerID).containsKey(movieName)) {
            return clientMovies.get(customerID).get(movieName).remove(movieID);
        } else {
            return false;
        }
    }

    private synchronized void insertCustomerAndMovie(String customerID, String movieName, String movieID) {
        Map<String, List<String>> temp = new ConcurrentHashMap<>();
        List<String> temp2 = new ArrayList<>();
        temp2.add(movieID);
        temp.put(movieName, temp2);
        clientMovies.put(customerID, temp);
    }

    private synchronized void insertMovieSlotsTypeAndMovie(String customerID, String movieName, String movieID) {
        List<String> temp = new ArrayList<>();
        temp.add(movieID);
        clientMovies.get(customerID).put(movieName, temp);
    }

    private boolean isCustomerOfThisServer(String customerID) {
        return customerID.substring(0, 3).equals(serverID);
    }

    private boolean onTheSameWeek(String newMovieDate, String movieID) {
        if (movieID.substring(6, 8).equals(newMovieDate.substring(2, 4)) && movieID.substring(8, 10).equals(newMovieDate.substring(4, 6))) {
            int week1 = Integer.parseInt(movieID.substring(4, 6)) / 7;
            int week2 = Integer.parseInt(newMovieDate.substring(0, 2)) / 7;
            return week1 == week2;
        } else {
            return false;
        }
    }

    public void insertNewMovie(String movieID, String movieName, int capacity) {
        MovieModel sampleConf = new MovieModel(movieName, movieID, capacity);
        allMovies.get(movieName).put(movieID, sampleConf);
    }

    public void insertNewCustomerToClients(String customerID) {
        ClientModel newCustomer = new ClientModel(customerID);
        serverClients.put(newCustomer.getClientID(), newCustomer);
        clientMovies.put(newCustomer.getClientID(), new ConcurrentHashMap<>());
    }
}
