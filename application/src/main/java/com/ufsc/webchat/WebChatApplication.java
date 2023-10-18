package com.ufsc.webchat;

import static java.lang.Integer.parseInt;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.Server;
import com.ufsc.webchat.server.WebChatClientHandler;
import com.ufsc.webchat.server.WebChatManagerThread;
import com.ufsc.webchat.server.WebChatServerHandler;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");

		EntityManagerProvider.init();

		WebChatServerHandler serverHandler = new WebChatServerHandler();
		WebChatClientHandler clientHandler = new WebChatClientHandler();
		WebChatManagerThread managerThread = new WebChatManagerThread(serverHandler, clientHandler);

		serverHandler.setManager(managerThread);
		clientHandler.setManager(managerThread);

		Server webServer = new Server(serverHandler, clientHandler, managerThread);

		webServer.start(parseInt(System.getProperty("applicationPort")));
	}
}
