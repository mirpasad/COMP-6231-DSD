package com.web.service;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

public class TestData {
	public static Service atwaterService;
	public static Service outremontService;
	public static Service verdunService;
	public static final String AVATAR = "Avatar";
	public static final String AVENGERS = "Avengers";
	public static final String TITANIC = "Titanic";

	public static void main(String[] args) throws Exception {
//
		URL atwaterURL = new URL("http://localhost:8080/montreal?wsdl");
		QName atwaterQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
		atwaterService = Service.create(atwaterURL, atwaterQName);

		URL verdunURL = new URL("http://localhost:8080/quebec?wsdl");
		QName verdunQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
		verdunService = Service.create(verdunURL, verdunQName);

		URL outremontURL = new URL("http://localhost:8080/sherbrook?wsdl");
		QName outremontQName = new QName("http://implementaion.service.web.com/", "MovieManagementService");
		outremontService = Service.create(outremontURL, outremontQName);

		addTestData();
	}

	private synchronized static void addTestData() {
		WebInterface ATWobj = atwaterService.getPort(WebInterface.class);
		WebInterface VERobj = verdunService.getPort(WebInterface.class);
		WebInterface OUTobj = outremontService.getPort(WebInterface.class);

		System.out.println("PreDemo TestCases");
		System.out.println("*********************************************************");

		System.out.println("Logged in as ATWM3456 MANAGER:");
		System.out.println(ATWobj.addMovie("ATWA080820", AVATAR, 2));
		System.out.println(ATWobj.addMovie("ATWM110820", AVENGERS, 1));
		System.out.println(ATWobj.addMovie("ATWM120820", AVENGERS, 1));

		System.out.println("Logged in as OUTM9000 MANAGER:");
		System.out.println(OUTobj.addMovie("OUTE080820", TITANIC, 1));

		System.out.println("Logged in as VERM9000 MANAGER:");
		System.out.println(VERobj.addMovie("VERA250820", AVENGERS, 1));
		System.out.println(VERobj.addMovie("VERE150820", TITANIC, 1));

		System.out.println("Logged in as OUTC1234 CUSTOMER:");
		System.out.println(OUTobj.bookMovie("OUTC1234", "ATWA080820", AVATAR));
		System.out.println(OUTobj.bookMovie("OUTC1234", "ATWM110820", AVENGERS));
		System.out.println(OUTobj.bookMovie("OUTC1234", "VERA250820", AVENGERS));
		System.out.println(OUTobj.bookMovie("OUTC1234", "OUTE080820", TITANIC));

	}
}
