package DataModel;

import java.util.ArrayList;
import java.util.List;

import static ServerInterface.MovieM.*;

public class MovieModel {
    public static final String MovieMorningSlot = "Morning";
    public static final String MovieAfternoonSlot = "Afternoon";
    public static final String MovieEveningSlot = "Evening";
    public static final String Avatar = "Avatar";
    public static final String Avengers = "Avengers";
    public static final String Titanic = "Titanic";
    public static final int TicketsFull = -1;
    public static final int ALREADY_REGISTERED = 0;
    public static final int ADD_SUCCESS = 1;
    private String movieName;
    private String movieID;
    private String movieServer;
    private int movieCapacity;
    private String movieDate;
    private String movieTimeSlot;
    private List<String> registeredClients;

    public MovieModel(String movieName, String movieID, int movieCapacity) {
        this.movieID = movieID;
        this.movieName = movieName;
        this.movieCapacity = movieCapacity;
        this.movieTimeSlot = detectMovieTimeSlot(movieID);
        this.movieServer = detectMovieServer(movieID);
        this.movieDate = detectMovieDate(movieID);
        registeredClients = new ArrayList<>();
    }

    public static String detectMovieServer(String movieID) {
        if (movieID.substring(0, 3).equalsIgnoreCase("ATW")) {
            return ATWMovieServer;
        } else if (movieID.substring(0, 3).equalsIgnoreCase("VER")) {
            return VERMovieServer;
        } else {
            return OUTMovieServer;
        }
    }

    public static String detectMovieTimeSlot(String movieID) {
        if (movieID.substring(3, 4).equalsIgnoreCase("M")) {
            return MovieMorningSlot;
        } else if (movieID.substring(3, 4).equalsIgnoreCase("A")) {
            return MovieAfternoonSlot;
        } else {
            return MovieEveningSlot;
        }
    }

    public static String detectMovieDate(String movieID) {
        return movieID.substring(4, 6) + "/" + movieID.substring(6, 8) + "/20" + movieID.substring(8, 10);
    }

    public String getMovieType() {
        return movieName;
    }

    public String getMovieID() {
        return movieID;
    }

    public void setMovieID(String movieID) {
        this.movieID = movieID;
    }
    public int getMovieCapacity() {
        return movieCapacity;
    }
    public void setMovieCapacity(int movieCapacity) {
        this.movieCapacity = movieCapacity;
    }
    public int getMovieRemainCapacity() {
        return movieCapacity;
    }
    public String getMovieDate() {
        return movieDate;
    }
    public String getMovieTimeSlot() {
        return movieTimeSlot;
    }

    public boolean isFull() {
        return getMovieCapacity() == registeredClients.size();
    }
    public List<String> getRegisteredClientIDs() {
        return registeredClients;
    }


    public int InsertRegisteredClientID(String registeredClientID) {
        if (!isFull()) {
            if (registeredClients.contains(registeredClientID)) {
                return ALREADY_REGISTERED;
            } else {
                registeredClients.add(registeredClientID);
                return ADD_SUCCESS;
            }
        } else {
            return TicketsFull;
        }
    }

    public boolean removeRegisteredClientID(String registeredClientID) {
        return registeredClients.remove(registeredClientID);
    }

    @Override
    public String toString() {
        return " (" + getMovieID() + ") in the " + getMovieTimeSlot() + " of " + getMovieDate() + " Total[Remaining] Capacity: " + getMovieCapacity() + "[" + getMovieRemainCapacity() + "]";
    }
}
