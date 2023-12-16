package com.web.service;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface WebInterface {
    /**
     * Only admin
     */
    public String addMovie(String movieID, String movieName, int bookingCapacity);

    public String removeMovie(String movieID, String movieName);

    public String listMovieAvailability(String movieName);

    /**
     * Both admin and Customer
     */
    public String bookMovie(String customerID, String movieID, String movieName);

    public String getBookingSchedule(String customerID);

    public String cancelMovie(String customerID, String movieID, String movieName);

    public String swapMovie(String customerID, String newMovieID, String newMovieName, String oldMovieID, String oldMovieName);

}
