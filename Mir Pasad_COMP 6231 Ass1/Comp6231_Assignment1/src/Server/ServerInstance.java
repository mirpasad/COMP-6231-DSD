package Server;

import Client.Client;
import DataModel.MovieModel;
import Logger.Logger;
import ServerInterface.MovieM;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//Variable Declaration
public class ServerInstance {

    private String serverID;
    private String serverName;
    private int serverRegistryPort;
    private int serverUdpPort;

    public ServerInstance(String serverID) throws Exception {
        this.serverID = serverID;
        switch (serverID) {
            case "ATW":
                serverName = MovieM.ATWMovieServer;
                serverRegistryPort = Client.ServerATW;
                serverUdpPort = MovieM.ATWServerPort;
                break;
            case "VER":
                serverName = MovieM.VERMovieServer;
                serverRegistryPort = Client.ServerVER;
                serverUdpPort = MovieM.VERServerPort;
                break;
            case "OUT":
                serverName = MovieM.OUTMovieServer;
                serverRegistryPort = Client.ServerOUT;
                serverUdpPort = MovieM.OUTServerPort;
                break;
        }

        MovieM remoteObject = new MovieM(serverID, serverName);
        Registry registry = LocateRegistry.createRegistry(serverRegistryPort);
        registry.bind(Client.registeredName, remoteObject);

        System.out.println(serverName + " Server is Up & Running");
        Logger.serverLog(serverID, " Server is Up & Running");
        InsertTestData(remoteObject);
        Runnable task = () -> {
            HearForRequest(remoteObject, serverUdpPort, serverName, serverID);
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    //adding some testdata to insert movies
    private void InsertTestData(MovieM remoteObject) {
        switch (serverID) {
            case "ATW":
                remoteObject.InsertMovie("ATWA010223", MovieModel.Avatar, 3);
                remoteObject.InsertMovie("ATWA030223", MovieModel.Titanic, 3);
                remoteObject.InsertMovie("ATWE050223", MovieModel.Avengers, 3);
                remoteObject.InsertMovie("ATWM070223", MovieModel.Titanic, 3);
                remoteObject.InsertMovie("ATWM080223", MovieModel.Avatar, 3);
                remoteObject.InsertMovie("ATWM090223", MovieModel.Avengers, 3);
                break;
            case "VER":
                remoteObject.InsertNewCustomerToClients("VERC1111");
                remoteObject.InsertNewCustomerToClients("VERC4444");
                break;
            case "OUT":
                remoteObject.InsertMovie("OUTE040223", MovieModel.Avatar, 3);
                remoteObject.InsertMovie("OUTE080223", MovieModel.Avatar, 3);
                remoteObject.InsertMovie("OUTM180223`", MovieModel.Avengers, 3);
                remoteObject.InsertMovie("OUTA230223", MovieModel.Titanic, 3);
                break;
        }
    }

    private static void HearForRequest(MovieM obj, int serverUdpPort, String serverName, String serverID) {
        DatagramSocket aSocket = null;
        String sendingResult = "";
        try {
            aSocket = new DatagramSocket(serverUdpPort);
            byte[] buffer = new byte[1000];
            System.out.println(serverName + " UDP Server Started at port " + aSocket.getLocalPort() + " ............");
            Logger.serverLog(serverID, " UDP Server Started at port " + aSocket.getLocalPort());
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                String sentence = new String(request.getData(), 0,
                        request.getLength());
                String[] parts = sentence.split(";");
                String method = parts[0];
                String customerID = parts[1];
                String movieName = parts[2];
                String movieID = parts[3];
                if (method.equalsIgnoreCase("removeMovieSlots")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.removeMovieSlotsUDP(movieID, movieName, customerID);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("displayAvailability")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " ", " ...");
                    String result = obj.displayAvailabilityUDP(movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("reserveMovieTickets")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.reserveMovieTickets(customerID, movieID, movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("cancelMovieTickets")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.cancelMovieTickets(customerID, movieID, movieName);
                    sendingResult = result + ";";
                }
                byte[] sendData = sendingResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendingResult.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
                Logger.serverLog(serverID, customerID, " UDP reply sent " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", sendingResult);
            }
        } catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
    }
}
