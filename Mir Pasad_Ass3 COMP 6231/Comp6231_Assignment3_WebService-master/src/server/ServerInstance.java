package server;

import DataModel.MovieModel;
import Logger.Logger;
import com.web.service.implementaion.MovieManagement;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerInstance {

    private String serverID;
    private String serverName;
    private String serverEndpoint;
    private int serverUdpPort;

    public ServerInstance(String serverID, String[] args) throws Exception {
        this.serverID = serverID;
        switch (serverID) {
            case "ATW":
                serverName = MovieManagement.ATWMovieServer;
                serverUdpPort = MovieManagement.Atwater_Server_Port;
                serverEndpoint = "http://localhost:8080/atwater";
                break;
            case "VER":
                serverName = MovieManagement.VERMovieServer;
                serverUdpPort = MovieManagement.Verdun_Server_Port;
                serverEndpoint = "http://localhost:8080/verdun";
                break;
            case "OUT":
                serverName = MovieManagement.OUTMovieServer;
                serverUdpPort = MovieManagement.Outremont_Server_Port;
                serverEndpoint = "http://localhost:8080/outremont";
                break;
        }
        try {
            System.out.println(serverName + " Server Started...");
            Logger.serverLog(serverID, " Server Started...");
            MovieManagement service = new MovieManagement(serverID, serverName);

            Endpoint endpoint = Endpoint.publish(serverEndpoint, service);

            System.out.println(serverName + " Server is Up & Running");
            Logger.serverLog(serverID, " Server is Up & Running");

            Runnable task = () -> {
                listenForRequest(service, serverUdpPort, serverName, serverID);
            };
            Thread thread = new Thread(task);
            thread.start();

        } catch (Exception e) {
            e.printStackTrace(System.out);
            Logger.serverLog(serverID, "Exception: " + e);
        }


    }

    private static void listenForRequest(MovieManagement obj, int serverUdpPort, String serverName, String serverID) {
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
                if (method.equalsIgnoreCase("removeMovie")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.removeMovieUDP(movieID, movieName, customerID);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("listMovieAvailability")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " ", " ...");
                    String result = obj.listMovieAvailabilityUDP(movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("bookMovie")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.bookMovie(customerID, movieID, movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("cancelMovie")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.cancelMovie(customerID, movieID, movieName);
                    sendingResult = result + ";";
                }
                sendingResult = sendingResult.trim();
                byte[] sendData = sendingResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendingResult.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
                Logger.serverLog(serverID, customerID, " UDP reply sent " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", sendingResult);
            }
        } catch (SocketException e) {
            System.err.println("SocketException: " + e);
            e.printStackTrace(System.out);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
            e.printStackTrace(System.out);
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
    }



}

