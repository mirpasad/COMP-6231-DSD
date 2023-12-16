package DataModel;

// Variable Declaration
public class ClientModel {
    public static final String adminClient = "ADMIN";
    public static final String customerClient = "CUSTOMER";
    public static final String ClientServerAtwater = "ATWATER";
    public static final String ClientServerVerdun = "VERDUN";
    public static final String ClientServerOutremont = "OUTREMONT";
    private String clientType;
    private String clientID;
    private String clientServer;

    public ClientModel(String clientID) {
        this.clientID = clientID;
        this.clientType = detectClientType();
        this.clientServer = detectClientServer();
    }

    //Detect which server to connect
    private String detectClientServer() {
        if (clientID.substring(0, 3).equalsIgnoreCase("ATW")) {
            return ClientServerAtwater;
        } else if (clientID.substring(0, 3).equalsIgnoreCase("VER")) {
            return ClientServerVerdun;
        } else {
            return ClientServerOutremont;
        }
    }

    //Loggen in as Customer or Admin
    private String detectClientType() {
        if (clientID.substring(3, 4).equalsIgnoreCase("A")) {
            return adminClient;
        } else {
            return customerClient;
        }
    }

    public String getClientType() {
        return clientType;
    }

    public String getClientID() {
        return clientID;
    }

    public String getClientServer() {
        return clientServer;
    }

    @Override
    public String toString() {
        return getClientType() + "(" + getClientID() + ") on " + getClientServer() + " Server.";
    }
}
