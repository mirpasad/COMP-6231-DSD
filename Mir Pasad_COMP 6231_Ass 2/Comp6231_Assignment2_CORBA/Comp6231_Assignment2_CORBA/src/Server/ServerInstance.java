package Server;

import DataModel.MovieModel;
import Logger.Logger;
import ServerInterface.MovieM;
import ServerObjectInterfaceApp.ServerObjectInterface;
import ServerObjectInterfaceApp.ServerObjectInterfaceHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerInstance {

    private String serverID;
    private String serverName;
    private int serverUdpPort;

    public ServerInstance(String serverID, String[] args) throws Exception {
        this.serverID = serverID;
        switch (serverID) {
            case "ATW":
                serverName = MovieM.ATWMovieServer;
                serverUdpPort = MovieM.ATWServerPort;
                break;
            case "VER":
                serverName = MovieM.VERMovieServer;
                serverUdpPort = MovieM.VERServerPort;
                break;
            case "OUT":
                serverName = MovieM.OUTMovieServer;
                serverUdpPort = MovieM.OUTServerPort;
                break;
        }
        try {
            // create and initialize the ORB //// get reference to rootpoa &amp; activate
            // the POAAdmin
            ORB orb = ORB.init(args, null);
            // -ORBInitialPort 1050 -ORBInitialHost localhost
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            MovieM servant = new MovieM(serverID, serverName);
            servant.setORB(orb);

            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(servant);
            ServerObjectInterface href = ServerObjectInterfaceHelper.narrow(ref);

            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            NameComponent[] path = ncRef.to_name(serverID);
            ncRef.rebind(path, href);

            System.out.println(serverName + " Server is Up & Running");
            Logger.serverLog(serverID, " Server is Up & Running");

                addTestData(servant);
            Runnable task = () -> {
                HearForRequest(servant, serverUdpPort, serverName, serverID);
            };
            Thread thread = new Thread(task);
            thread.start();

            while (true) {
                orb.run();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            Logger.serverLog(serverID, "Exception: " + e);
        }

        System.out.println(serverName + " Server Shutting down");
        Logger.serverLog(serverID, " Server Shutting down");

    }

    private void addTestData(MovieM remoteObject) {
        switch (serverID) {
            case "ATW":
                remoteObject.insertNewMovie("ATWA090620", MovieModel.Avatar, 2);
                remoteObject.insertNewMovie("ATWA080620", MovieModel.Titanic, 2);
                remoteObject.insertNewMovie("ATWE230620", MovieModel.Avengers, 1);
                remoteObject.insertNewMovie("ATWA150620", MovieModel.Titanic, 12);
                break;
            case "VER":
                remoteObject.insertNewCustomerToClients("VERC1234");
                remoteObject.insertNewCustomerToClients("VERC4114");
                break;
            case "OUT":
                remoteObject.insertNewMovie("OUTE110620", MovieModel.Avatar, 1);
                remoteObject.insertNewMovie("OUTE080620", MovieModel.Avatar, 1);
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
                Integer qTickets = 0;
                if (method.equalsIgnoreCase("removeMovieSlots")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.removeMovieSlotsSlotsUDP(movieID, movieName, customerID);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("displayAvailability")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " ", " ...");
                    String result = obj.displayAvailabilityUDP(movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("reserveMovieTickets")) {
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieName: " + movieName + " ", " ...");
                    String result = obj.reserveMovieTickets(customerID, movieID, movieName, qTickets);
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
