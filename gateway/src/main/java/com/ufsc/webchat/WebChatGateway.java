package com.ufsc.webchat;

import static java.lang.Integer.parseInt;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.ServerHandler;
import com.ufsc.webchat.server.Server;

public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");

		EntityManagerProvider.init();

		ServerHandler serverHandler = new ServerHandler();
		Server webServer = new Server(serverHandler);

		webServer.start(parseInt(System.getProperty("gatewayPort")));
	}
}
