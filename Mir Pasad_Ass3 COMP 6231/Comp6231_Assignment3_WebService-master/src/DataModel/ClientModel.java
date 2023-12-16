package DataModel;

public class ClientModel {
    public static final String adminClient = "ADMIN";
    public static final String customerClient = "CUSTOMER";
    public static final String ClientServerOutremont = "Outremont";
    public static final String ClientServerVerdun = "Verdun";
    public static final String ClientServerAtwater = "Atwater";
    private String clientType;
    private String clientID;
    private String clientServer;

    public ClientModel(String clientID) {
        this.clientID = clientID;
        this.clientType = detectClientType();
        this.clientServer = detectClientServer();
    }

    private String detectClientServer() {
        if (clientID.substring(0, 3).equalsIgnoreCase("ATW")) {
            return ClientServerAtwater;
        } else if (clientID.substring(0, 3).equalsIgnoreCase("VER")) {
            return ClientServerVerdun;
        } else {
            return ClientServerOutremont;
        }
    }

    private String detectClientType() {
        if (clientID.substring(3, 4).equalsIgnoreCase("M")) {
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
