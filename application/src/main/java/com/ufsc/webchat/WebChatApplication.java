package com.ufsc.webchat;

import com.ufsc.database.EntityManagerProvider;
import com.ufsc.database.PropertyLoader;
import com.ufsc.webchat.config.NetworkConfiguration;
import com.ufsc.webchat.server.Server;
import com.ufsc.webchat.server.WebChatClientHandler;
import com.ufsc.webchat.server.WebChatManager;
import com.ufsc.webchat.server.WebChatServerHandler;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");
		EntityManagerProvider.init();

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