package ServerInterface;

import DataModel.ClientModel;
import DataModel.MovieModel;
import Interface.MovieInterface;
import Logger.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MovieM extends UnicastRemoteObject implements MovieInterface {
    public static final int ATWServerPort = 1322;
    public static final int VERServerPort = 2213;
    public static final int OUTServerPort = 2132;
    public static final String OUTMovieServer = "OUTREMONT";
    public static final String VERMovieServer = "VERDUN";
    public static final String ATWMovieServer = "ATWATER";
    private String serverID;
    private String serverName;
    private Map<String, Map<String, MovieModel>> allMovies;
    private Map<String, Map<String, List<String>>> clientMovies;
    private Map<String, ClientModel> serverClients;
    public MovieM(String serverID, String serverName) throws RemoteException {
        super();
        this.serverID = serverID;
        this.serverName = serverName;
        allMovies = new ConcurrentHashMap<>();
        allMovies.put(MovieModel.Avatar, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.Avengers, new ConcurrentHashMap<>());
        allMovies.put(MovieModel.Titanic, new ConcurrentHashMap<>());
        clientMovies = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();

    }
    

    private static int getServerPort(String branch) {
        if (branch.equalsIgnoreCase("ATW")) {
            return ATWServerPort;
        } else if (branch.equalsIgnoreCase("OUT")) {
            return OUTServerPort;
        } else if (branch.equalsIgnoreCase("VER")) {
            return VERServerPort;
        }
        return 1;
    }

    //Admin can insert movie and capacity for a movie with timing
    //if the movie is added then admin cannot change the capacity
    //Admin can only add the particular movie to its own server
    @Override
    public String insertMovieslots(String movieID, String movieName, int bookingCapacity) throws RemoteException {
        String response;
        if (allMovies.get(movieName).containsKey(movieID)) {
            if (allMovies.get(movieName).get(movieID).getMovieCapacity() <= bookingCapacity) {
                allMovies.get(movieName).get(movieID).setMovieCapacity(bookingCapacity);
                response = "Success: Movie " + movieID + " The capacity grew to " + bookingCapacity;
                try {
                    Logger.serverLog(serverID, "null", " RMI InsertMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failure: Cannot Lower Booking Capacity Due to Existence of the Film";
                try {
                    Logger.serverLog(serverID, "null", " RMI InsertMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
        if (MovieModel.detectMovieServer(movieID).equals(serverName)) {
            MovieModel event = new MovieModel(movieName, movieID, bookingCapacity);
            Map<String, MovieModel> eventHashMap = allMovies.get(movieName);
            eventHashMap.put(movieID, event);
            allMovies.put(movieName, eventHashMap);
            response = "Success: Movie " + movieID + " added successfully";
            try {
                Logger.serverLog(serverID, "null", " RMI InsertMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        } else {
            response = "Failed: Unable to insert movie to servers besides" + serverName;
            try {
                Logger.serverLog(serverID, "null", " RMI InsertMovie ", " movieID: " + movieID + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    //Admin can remove any particular only but only from the particular movie server
    // If the movie is not added and Admin tries to remove it then error will be displayed

    @Override
    public String removeMovieSlots(String movieID, String movieName) throws RemoteException {
        String response;
        if (MovieModel.detectMovieServer(movieID).equals(serverName)) {
            if (allMovies.get(movieName).containsKey(movieID)) {
                List<String> registeredClients = allMovies.get(movieName).get(movieID).getRegisteredClientIDs();
                allMovies.get(movieName).remove(movieID);
                insertCustomersToNextSameMovie(movieID, movieName, registeredClients);
                response = "Success: The Movie was successfully deleted";
                try {
                    Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " Does Not Exist";
                try {
                    Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            response = "Failed: Unable to remove the Movie from any additional servers " + serverName;
            try {
                Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    //if wrong movie name then displays error
    //Displays each movie name at all servers which every detailed information
    @Override
    public String displayAvailability(String movieName) throws RemoteException {
        String response;
        Map<String, MovieModel> movies = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ":\n");
        if (movies.size() == 0) {
            builder.append("No Movies of Type " + movieName);
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movies.toString() + " || ");
            }
        }
        String otherServer1, otherServer2;
        if (serverID.equals("ATW")) {
            otherServer1 = sendUDPMessage(OUTServerPort, "displayAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(VERServerPort, "displayAvailability", "null", movieName, "null");
        } else if (serverID.equals("OUT")) {
            otherServer1 = sendUDPMessage(VERServerPort, "displayAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(ATWServerPort, "displayAvailability", "null", movieName, "null");
        } else {
            otherServer1 = sendUDPMessage(ATWServerPort, "displayAvailability", "null", movieName, "null");
            otherServer2 = sendUDPMessage(OUTServerPort, "displayAvailability", "null", movieName, "null");
        }
        builder.append(otherServer1).append(otherServer2);
        response = builder.toString();
        try {
            Logger.serverLog(serverID, "null", " RMI displayAvailability ", " movieName: " + movieName + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    //Customer can book movie ticket by specifying the movieID
    // If the capacity is full for a particular movie customer is trying to book then error will be displayed
    //Customer can book any number of tickets for its own server depending on the capacity
    //Customer can book no more than 3 tickets for other server in a week
    @Override
    public String reserveMovieTickets(String customerID, String movieID, String movieName) throws RemoteException {
        String response;
        if (!serverClients.containsKey(customerID)) {
            InsertNewCustomerToClients(customerID);
        }
        if (MovieModel.detectMovieServer(movieID).equals(serverName)) {
            MovieModel bookedMovie = allMovies.get(movieName).get(movieID);
            if (!bookedMovie.isFull()) {
                if (clientMovies.containsKey(customerID)) {
                    if (clientMovies.get(customerID).containsKey(movieName)) {
                        if (!clientMovies.get(customerID).get(movieName).contains(movieID)) {
                            clientMovies.get(customerID).get(movieName).add(movieID);
                        } else {
                            response = "Failed: Movie " + movieID + " Already Booked";
                            try {
                                Logger.serverLog(serverID, customerID, " RMI reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                    } else {
                        List<String> temp = new ArrayList<>();
                        temp.add(movieID);
                        clientMovies.get(customerID).put(movieName, temp);
                    }
                } else {
                    Map<String, List<String>> temp = new ConcurrentHashMap<>();
                    List<String> temp2 = new ArrayList<>();
                    temp2.add(movieID);
                    temp.put(movieName, temp2);
                    clientMovies.put(customerID, temp);
                }
                if (allMovies.get(movieName).get(movieID).InsertRegisteredClientID(customerID) == MovieModel.ADD_SUCCESS) {
                    response = "Success: Movie " + movieID + " Booked Successfully";
                } else if (allMovies.get(movieName).get(movieID).InsertRegisteredClientID(customerID) == MovieModel.TicketsFull) {
                    response = "Failed: Movie " + movieID + " is Full";
                } else {
                    response = "Failed: Cannot Insert You To Movie " + movieID;
                }
                try {
                    Logger.serverLog(serverID, customerID, " RMI reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } else {
                response = "Failed: Movie " + movieID + " is Full";
                try {
                    Logger.serverLog(serverID, customerID, " RMI reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        } else {
            if (!exceedWeeklyLimit(customerID, movieID.substring(4))) {
                String serverResponse = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "reserveMovieTickets", customerID, movieName, movieID);
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
                    Logger.serverLog(serverID, customerID, " RMI reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return serverResponse;
            } else {
                response = "Failed: You Cannot Book Movie in Other Servers For This Week(Max Weekly Limit = 3)";
                try {
                    Logger.serverLog(serverID, customerID, " RMI reserveMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    //Customer can check the booking schedule he has made
    //If no tickets purchased then he will get empty booking
    @Override
    public String obtainBookingSchedule(String customerID) throws RemoteException {
        String response;
        if (!serverClients.containsKey(customerID)) {
            InsertNewCustomerToClients(customerID);
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " RMI obtainBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        Map<String, List<String>> events = clientMovies.get(customerID);
        if (events.size() == 0) {
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " RMI obtainBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        StringBuilder builder = new StringBuilder();
        for (String movieName :
                events.keySet()) {
            builder.append(movieName + ":\n");
            for (String movieID :
                    events.get(movieName)) {
                builder.append(movieID + " ||");
            }
        }
        response = builder.toString();
        try {
            Logger.serverLog(serverID, customerID, " RMI obtainBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    //Customer can cancel the tickets purchased for movies
    //if customer tries to cancel booking for any reservation that he has not made then there will be an error

    @Override
    public String cancelMovieTickets(String customerID, String movieID, String movieName) throws RemoteException {
        String response;
        if (MovieModel.detectMovieServer(movieID).equals(serverName)) {
            if (customerID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(customerID)) {
                    InsertNewCustomerToClients(customerID);
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    if (clientMovies.get(customerID).get(movieName).remove(movieID)) {
                        allMovies.get(movieName).get(movieID).removeRegisteredClientID(customerID);
                        response = "Success: Movie " + movieID + " Canceled for " + customerID;
                        try {
                            Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    } else {
                        response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                        try {
                            Logger.serverLog(serverID, customerID, " RMI cancelMovieTickets ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
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
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } else {
                    response = "Failed: You " + customerID + " Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }
            }
        } else {
            if (customerID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(customerID)) {
                    InsertNewCustomerToClients(customerID);
                } else {
                    if (clientMovies.get(customerID).get(movieName).remove(movieID)) {
                        return sendUDPMessage(getServerPort(movieID.substring(0, 3)), "cancelMovieTickets", customerID, movieName, movieID);
                    }
                }
            }
            return "Failed: You " + customerID + " Are Not Registered in " + movieID;
        }
    }
    
    public String removeMovieSlotsUDP(String oldMovieID, String movieName, String customerID) throws RemoteException {
        if (!serverClients.containsKey(customerID)) {
            InsertNewCustomerToClients(customerID);
            return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
        } else {
            if (clientMovies.get(customerID).get(movieName).remove(oldMovieID)) {
                return "Success: Movie " + oldMovieID + " Was Removed from " + customerID + " Schedule";
            } else {
                return "Failed: You " + customerID + " Are Not Registered in " + oldMovieID;
            }
        }
    }
    
    public String displayAvailabilityUDP(String movieName) throws RemoteException {
        Map<String, MovieModel> events = allMovies.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ":\n");
        if (events.size() == 0) {
            builder.append("No Movies of Type " + movieName);
        } else {
            for (MovieModel event :
                    events.values()) {
                builder.append(event.toString() + " || ");
            }
        }
        return builder.toString();
    }

    private String sendUDPMessage(int serverPort, String method, String customerID, String movieName, String movieID) {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + customerID + ";" + movieName + ";" + movieID;
        try {
            Logger.serverLog(serverID, customerID, " UDP request sent " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ... ");
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
            Logger.serverLog(serverID, customerID, " UDP reply received" + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", result);
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
        for (int i = 0; i <= 3; i++) {
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
                if (movieID.substring(6, 8).equals(movieDate.substring(2, 4)) && movieID.substring(8, 10).equals(movieDate.substring(4, 6))) {
                    int week1 = Integer.parseInt(movieID.substring(4, 6)) / 7;
                    int week2 = Integer.parseInt(movieDate.substring(0, 2)) / 7;
                    if (week1 == week2) {
                        limit++;
                    }
                }
                if (limit == 3)
                    return true;
            }
        }
        return false;
    }

    private void insertCustomersToNextSameMovie(String oldMovieID, String movieName, List<String> registeredClients) throws RemoteException {
        for (String customerID :
                registeredClients) {
            if (customerID.substring(0, 3).equals(serverID)) {
                clientMovies.get(customerID).get(movieName).remove(oldMovieID);
                String nextSameMovieResult = getNextSameMovie(allMovies.get(movieName).keySet(), movieName, oldMovieID);
                if (nextSameMovieResult.equals("Failed")) {
                    return;
                } else {
                    reserveMovieTickets(customerID, nextSameMovieResult, movieName);
                }
            } else {
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeMovie", customerID, movieName, oldMovieID);
            }
        }
    }

    public void InsertMovie(String movieID, String movieName, int capacity) {
        MovieModel sampleConf = new MovieModel(movieName, movieID, capacity);
        allMovies.get(movieName).put(movieID, sampleConf);
    }

    public void InsertNewCustomerToClients(String customerID) {
        ClientModel newCustomer = new ClientModel(customerID);
        serverClients.put(newCustomer.getClientID(), newCustomer);
        clientMovies.put(newCustomer.getClientID(), new ConcurrentHashMap<>());
    }
}
