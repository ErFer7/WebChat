package com.ufsc.webchat;

import static java.lang.Integer.parseInt;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.GatewayServerHandler;
import com.ufsc.webchat.server.Server;

public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");

		EntityManagerProvider.init();

		GatewayServerHandler serverHandler = new GatewayServerHandler();
		Server webServer = new Server(serverHandler);

		webServer.start(parseInt(System.getProperty("gatewayPort")));
	}
}
