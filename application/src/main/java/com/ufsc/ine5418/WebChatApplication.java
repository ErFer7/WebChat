package com.ufsc.ine5418;

import com.ufsc.ine5418.config.NetworkConfiguration;
import com.ufsc.ine5418.database.DatabaseConnection;
import com.ufsc.ine5418.database.PropertyLoader;
import com.ufsc.ine5418.server.Server;
import com.ufsc.ine5418.server.WebChatClientHandler;
import com.ufsc.ine5418.server.WebChatManager;
import com.ufsc.ine5418.server.WebChatServerHandler;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		new PropertyLoader().loadProperties();
		DatabaseConnection dbConnection = new DatabaseConnection();
		dbConnection.connect();
		
		NetworkConfiguration config = new NetworkConfiguration();

		WebChatServerHandler webChatServerHandler = new WebChatServerHandler();
		WebChatClientHandler webChatClientHandler = new WebChatClientHandler(config.getGatewayHost(), config.getGatewayPort());
		WebChatManager webChatManager = new WebChatManager(webChatServerHandler, webChatClientHandler, config.getGatewayIdentifier(), config.getGatewayPassword());

		webChatServerHandler.setManager(webChatManager);
		webChatClientHandler.setManager(webChatManager);

		Server webServer = new Server(webChatServerHandler, webChatClientHandler, webChatManager);

		webServer.start(config.getApplicationPort());
	}
}
