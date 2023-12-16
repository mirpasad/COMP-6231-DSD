package com.web.service.implementaion;

import DataModel.ClientModel;
import DataModel.MovieModel;
import Logger.Logger;
import com.web.service.WebInterface;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebService(endpointInterface = "com.web.service.WebInterface")

@SOAPBinding(style = SOAPBinding.Style.RPC)
public class MovieManagement implements WebInterface {
    public static final int Atwater_Server_Port = 8888;
    public static final int Verdun_Server_Port = 7777;
    public static final int Outremont_Server_Port = 6666;
    public static final String OUTMovieServer = "Outremont";
    public static final String VERMovieServer = "Verdun";
    public static final String ATWMovieServer = "Atwater";
    private String serverID;
    private String serverName;
    private Map<String, Map<String, MovieModel>> allMovies;
    private Map<String, Map<String, List<String>>> clientMovies;
    private Map<String, ClientModel> serverClients;


    public MovieManagement(String serverID, String serverName) {
        super();
        this.serverID = serverID;
        this.serverName = serverName;
        allMovies = new ConcurrentHashMap<>();
        allMovies.put(MovieModel.AVATAR, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.AVENGERS, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.TITANIC, new ConcurrentHashMap<>());
        clientMovies = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();
    }

    private static int getServerPort(String branchAcronym) {
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            return Atwater_Server_Port;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            return Outremont_Server_Port;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            return Verdun_Server_Port;
        }
        return 1;
    }


    @Override
    public String addMovie(String movieID, String movieName, int bookingCapacity) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (movieExists(movieName, movieID)) {
                if (allMovies.get(movieName).get(movieID).getMovieCapacity() <= bookingCapacity) {
                    allMovies.get(movieName).get(movieID).setMovieCapacity(bookingCapacity);
                    response = "Success: Movie " + movieID + " Capacity increased to " + bookingCapacity;
                    try {
                        Logger.serverLog(serverID, "null", "  addMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    response = "Failed: Movie Already Exists, Cannot Decrease Booking Capacity";
                    try {
                        Logger.serverLog(serverID, "null", "  addMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
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
                    Logger.serverLog(serverID, "null", "  addMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            response = "Failed: Cannot Add Movie to servers other than " + serverName;
            try {
                Logger.serverLog(serverID, "null", "  addMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    @Override
    public String removeMovie(String movieID, String movieName) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (movieExists(movieName, movieID)) {
                List<String> registeredClients = allMovies.get(movieName).get(movieID).getRegisteredClientIDs();
                allMovies.get(movieName).remove(movieID);
                addCustomersToNextSameMovie(movieID, movieName, registeredClients);
                response = "Success: Movie " + movieID + " Removed Successfully";
                try {
                    Logger.serverLog(serverID, "null", "  removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " Does Not Exist";
                try {
                    Logger.serverLog(serverID, "null", "  removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            response = "Failed: Cannot Remove Movie from servers other than " + serverName;
            try {
                Logger.serverLog(serverID, "null", "  removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    @Override
    public String listMovieAvailability(String movieName) {
        String response;
        Map<String, MovieModel> movies = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(" Server ").append(movieName).append(":\n");
        if (movies.size() == 0) {
            builder.append("No Movies of Type ").append(movieName).append("\n");
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movie.toString()).append(" || ");
            }
        }
        String otherServer1, otherServer2;
        if (serverID.equals("ATW")) {
            otherServer1 = sendUDPMessage(Outremont_Server_Port, "listMovieAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(Verdun_Server_Port, "listMovieAvailability", "null", movieName, "null");
        } else if (serverID.equals("OUT")) {
            otherServer1 = sendUDPMessage(Verdun_Server_Port, "listMovieAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(Atwater_Server_Port, "listMovieAvailability", "null", movieName, "null");
        } else {
            otherServer1 = sendUDPMessage(Atwater_Server_Port, "listMovieAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(Outremont_Server_Port, "listMovieAvailability", "null", movieName, "null");
        }
        builder.append(otherServer1).append(otherServer2);
        response = builder.toString();
        try {
            Logger.serverLog(serverID, "null", "  listMovieAvailability ", " movieName: " + movieName + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String bookMovie(String customerID, String movieID, String movieName) {
        String response;
        checkClientExists(customerID);
        if (isMovieOfThisServer(movieID)) {
            MovieModel bookedMovie = allMovies.get(movieName).get(movieID);
            if (bookedMovie == null) {
                response = "Failed: Movie " + movieID + " Does not exists";
                try {
                    Logger.serverLog(serverID, customerID, " bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
            if (!bookedMovie.isFull()) {
                if (clientMovies.containsKey(customerID)) {
                    if (clientMovies.get(customerID).containsKey(movieName)) {
                        if (!clientHasMovie(customerID, movieName, movieID)) {
                            if (isCustomerOfThisServer(customerID))
                                clientMovies.get(customerID).get(movieName).add(movieID);
                        } else {
                            response = "Failed: Movie " + movieID + " Already Booked";
                            try {
                                Logger.serverLog(serverID, customerID, "  bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                    } else {
                        if (isCustomerOfThisServer(customerID))
                            addMovieTypeAndMovie(customerID, movieName, movieID);
                    }
                } else {
                    if (isCustomerOfThisServer(customerID))
                        addCustomerAndMovie(customerID, movieName, movieID);
                }
                if (allMovies.get(movieName).get(movieID).addRegisteredClientID(customerID) == MovieModel.ADD_SUCCESS) {
                    response = "Success: Movie " + movieID + " Booked Successfully";
                } else if (allMovies.get(movieName).get(movieID).addRegisteredClientID(customerID) == MovieModel.TicketsFull) {
                    response = "Failed: Movie " + movieID + " is Full";
                } else {
                    response = "Failed: Cannot Add You To Movie " + movieID;
                }
                try {
                    Logger.serverLog(serverID, customerID, " bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " is Full";
                try {
                    Logger.serverLog(serverID, customerID, "  bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            if (clientHasMovie(customerID, movieName, movieID)) {
                String serverResponse = "Failed: Movie " + movieID + " Already Booked";
                try {
                    Logger.serverLog(serverID, customerID, "  bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return serverResponse;
            }
            if (!exceedWeeklyLimit(customerID, movieID.substring(4))) {
                String serverResponse = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "bookMovie", customerID, movieName, movieID);
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
                    Logger.serverLog(serverID, customerID, "  bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return serverResponse;
            } else {
                response = "Failed: You Cannot Book Movie in Other Servers For This Week(Max Weekly Limit = 3)";
                try {
                    Logger.serverLog(serverID, customerID, "  bookMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    @Override
    public String getBookingSchedule(String customerID) {
        String response;
        if (!checkClientExists(customerID)) {
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, "  getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        Map<String, List<String>> movies = clientMovies.get(customerID);
        if (movies.size() == 0) {
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, "  getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        StringBuilder builder = new StringBuilder();
        for (String movieName :
                movies.keySet()) {
            builder.append(movieName).append(":\n");
            for (String movieID :
                    movies.get(movieName)) {
                builder.append(movieID).append(" ||");
            }
        }
        response = builder.toString();
        try {
            Logger.serverLog(serverID, customerID, "  getBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String cancelMovie(String customerID, String movieID, String movieName) {
        String response;
        if (isMovieOfThisServer(movieID)) {
            if (isCustomerOfThisServer(customerID)) {
                if (!checkClientExists(customerID)) {
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    if (removeMovieIfExists(customerID, movieName, movieID)) {
                        allMovies.get(movieName).get(movieID).removeRegisteredClientID(customerID);
                        response = "Success: Movie " + movieID + " Canceled for " + customerID;
                        try {
                            Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    } else {
                        response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                        try {
                            Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    }
                }
            } else {
                if (allMovies.get(movieName).get(movieID).removeRegisteredClientID(customerID)) {
                    response = "Success: Movie " + movieID + " Canceled for " + customerID;
                    try {
                        Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }
            }
        } else {
            if (isCustomerOfThisServer(customerID)) {
                if (checkClientExists(customerID)) {
                    if (removeMovieIfExists(customerID, movieName, movieID)) {
                        response = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "cancelMovie", customerID, movieName, movieID);
                        try {
                            Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    }
                }
            }
            response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
            try {
                Logger.serverLog(serverID, customerID, "  cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    @Override
    public String swapMovie(String customerID, String newMovieID, String newMovieName, String oldMovieID, String oldMovieName) {
        String response;
        if (!checkClientExists(customerID)) {
            response = "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
            try {
                Logger.serverLog(serverID, customerID, "  swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        } else {
            if (clientHasMovie(customerID, oldMovieName, oldMovieID)) {
                String bookResp = "Failed: did not send book request for your newMovie " + newMovieID;
                String cancelResp = "Failed: did not send cancel request for your oldMovie " + oldMovieID;
                synchronized (this) {
                    if (onTheSameWeek(newMovieID.substring(4), oldMovieID) && exceedWeeklyLimit(customerID, newMovieID.substring(4))) {
                        cancelResp = cancelMovie(customerID, oldMovieID, oldMovieName);
                        if (cancelResp.startsWith("Success:")) {
                            bookResp = bookMovie(customerID, newMovieID, newMovieName);
                        }
                    } else {
                        bookResp = bookMovie(customerID, newMovieID, newMovieName);
                        if (bookResp.startsWith("Success:")) {
                            cancelResp = cancelMovie(customerID, oldMovieID, oldMovieName);
                        }
                    }
                }
                if (bookResp.startsWith("Success:") && cancelResp.startsWith("Success:")) {
                    response = "Success: Movie " + oldMovieID + " swapped with " + newMovieID;
                } else if (bookResp.startsWith("Success:") && cancelResp.startsWith("Failed:")) {
                    cancelMovie(customerID, newMovieID, newMovieName);
                    response = "Failed: Your oldMovie " + oldMovieID + " Could not be Canceled reason: " + cancelResp;
                } else if (bookResp.startsWith("Failed:") && cancelResp.startsWith("Success:")) {
                    //hope this won't happen, but just in case.
                    String resp1 = bookMovie(customerID, oldMovieID, oldMovieName);
                    response = "Failed: Your newMovie " + newMovieID + " Could not be Booked reason: " + bookResp + " And your old movie Rolling back: " + resp1;
                } else {
                    response = "Failed: on Both newMovie " + newMovieID + " Booking reason: " + bookResp + " and oldMovie " + oldMovieID + " Canceling reason: " + cancelResp;
                }
                try {
                    Logger.serverLog(serverID, customerID, "  swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
                try {
                    Logger.serverLog(serverID, customerID, "  swapMovie ", " oldMovieID: " + oldMovieID + " oldMovieName: " + oldMovieName + " newMovieID: " + newMovieID + " newMovieName: " + newMovieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    /**
     * for udp calls only
     *
     * @param oldNewMovieID
     * @param movieName
     * @param customerID
     * @return
     */
    public String removeMovieUDP(String oldNewMovieID, String movieName, String customerID) {
        String oldMovieID, newMovieID;
        String[] parts = oldNewMovieID.split(":");
        oldMovieID = parts[0];
        newMovieID = parts[1];
        if (!checkClientExists(customerID)) {
            return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
        } else {
            if (removeMovieIfExists(customerID, movieName, oldMovieID)) {
                if (!newMovieID.equalsIgnoreCase("null")) {
                    bookMovie(customerID, newMovieID, movieName);
                }
                return "Success: Movie " + oldMovieID + " Was Removed from " + customerID + " Schedule";
            } else {
                return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
            }
        }
    }

    /**
     * for UDP calls only
     *
     * @param movieName
     * @return
     */
    public String listMovieAvailabilityUDP(String movieName) {
        Map<String, MovieModel> movies = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(" Server ").append(movieName).append(":\n");
        if (movies.size() == 0) {
            builder.append("No Movies of Type ").append(movieName);
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movie.toString()).append(" || ");
            }
        }
        return builder.toString();
    }

    private String sendUDPMessage(int serverPort, String method, String customerID, String movieName, String movieId) {
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
            result = new String(reply.getData()).trim();
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
        List<String> sortedIDs = new ArrayList<>(keySet);
        sortedIDs.add(oldMovieID);
        sortedIDs.sort((ID1, ID2) -> {
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
            int timeSlot2 = 0;
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
                    if (clientMovies.get(customerID).containsKey(MovieModel.AVATAR)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.AVATAR);
                    }
                    break;
                case 1:
                    if (clientMovies.get(customerID).containsKey(MovieModel.AVENGERS)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.AVENGERS);
                    }
                    break;
                case 2:
                    if (clientMovies.get(customerID).containsKey(MovieModel.TITANIC)) {
                        registeredIDs = clientMovies.get(customerID).get(MovieModel.TITANIC);
                    }
                    break;
            }
            for (String movieID :
                    registeredIDs) {
                if (onTheSameWeek(movieDate, movieID) && !isMovieOfThisServer(movieID)) {
                    limit++;
                }
                if (limit == 3)
                    return true;
            }
        }
        return false;
    }

    private void addCustomersToNextSameMovie(String oldMovieID, String movieName, List<String> registeredClients) {
        for (String customerID :
                registeredClients) {
            if (customerID.substring(0, 3).equals(serverID)) {
                removeMovieIfExists(customerID, movieName, oldMovieID);
            }


            tryToBookNextSameMovie(customerID, movieName, oldMovieID);
        }
    }

    private void tryToBookNextSameMovie(String customerID, String movieName, String oldMovieID) {
        String response;
        String nextSameMovieResult = getNextSameMovie(allMovies.get(movieName).keySet(), movieName, oldMovieID);
        if (nextSameMovieResult.equals("Failed")) {
            if (!customerID.substring(0, 3).equals(serverID)) {
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeMovie", customerID, movieName, oldMovieID + ":null");
            }
            response = "Acquiring nextSameMovie for Client (" + customerID + "):" + nextSameMovieResult;
            try {
                Logger.serverLog(serverID, customerID, " addCustomersToNextSameMovie ", " oldMovieID: " + oldMovieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (customerID.substring(0, 3).equals(serverID)) {
                bookMovie(customerID, nextSameMovieResult, movieName);
            } else {
                String oldNewMovieID = oldMovieID + ":" + nextSameMovieResult;
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeMovie", customerID, movieName, oldNewMovieID);
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
            addNewCustomerToClients(customerID);
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

    private boolean removeMovieIfExists(String customerID, String movieName, String movieID) {
        if (clientMovies.get(customerID).containsKey(movieName)) {
            return clientMovies.get(customerID).get(movieName).remove(movieID);
        } else {
            return false;
        }
    }

    private synchronized void addCustomerAndMovie(String customerID, String movieName, String movieID) {
        Map<String, List<String>> temp = new ConcurrentHashMap<>();
        List<String> temp2 = new ArrayList<>();
        temp2.add(movieID);
        temp.put(movieName, temp2);
        clientMovies.put(customerID, temp);
    }

    private synchronized void addMovieTypeAndMovie(String customerID, String movieName, String movieID) {
        List<String> temp = new ArrayList<>();
        temp.add(movieID);
        clientMovies.get(customerID).put(movieName, temp);
    }

    private boolean isCustomerOfThisServer(String customerID) {
        return customerID.substring(0, 3).equals(serverID);
    }

    private boolean onTheSameWeek(String newMovieDate, String movieID) {
        if (movieID.substring(6, 8).equals(newMovieDate.substring(2, 4)) && movieID.substring(8, 10).equals(newMovieDate.substring(4, 6))) {
            int day1 = Integer.parseInt(movieID.substring(4, 6));
            int day2 = Integer.parseInt(newMovieDate.substring(0, 2));
            if (day1 % 7 == 0) {
                day1--;
            }
            if (day2 % 7 == 0) {
                day2--;
            }
            int week1 = day1 / 7;
            int week2 = day2 / 7;
            return week1 == week2;
        } else {
            return false;
        }
    }

    public Map<String, Map<String, MovieModel>> getAllMovies() {
        return allMovies;
    }

    public Map<String, Map<String, List<String>>> getClientMovies() {
        return clientMovies;
    }

    public Map<String, ClientModel> getServerClients() {
        return serverClients;
    }

    public void addNewMovie(String movieID, String movieName, int capacity) {
        MovieModel sampleConf = new MovieModel(movieName, movieID, capacity);
        allMovies.get(movieName).put(movieID, sampleConf);
    }

    public void addNewCustomerToClients(String customerID) {
        ClientModel newCustomer = new ClientModel(customerID);
        serverClients.put(newCustomer.getClientID(), newCustomer);
        clientMovies.put(newCustomer.getClientID(), new ConcurrentHashMap<>());
    }
}
