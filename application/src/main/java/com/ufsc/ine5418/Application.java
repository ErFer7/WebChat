package com.ufsc.ine5418;

import com.ufsc.ine5418.database.DatabaseConnection;
import com.ufsc.ine5418.database.PropertyLoader;
import com.ufsc.ine5418.web.WebServer;

public class Application {

	public static void main(String[] args) throws Exception {
		new PropertyLoader().loadProperties();
		DatabaseConnection dbConnection = new DatabaseConnection();
		dbConnection.connect();

		WebServer webServer = new WebServer();
		webServer.start(8080);
	}
}
