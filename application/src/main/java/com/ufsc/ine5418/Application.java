package com.ufsc.ine5418;

import com.ufsc.ine5418.web.WebServer;

public class Application {

	public static void main(String[] args) throws Exception {
		WebServer webServer = new WebServer();

		webServer.start(8080);
	}
}
