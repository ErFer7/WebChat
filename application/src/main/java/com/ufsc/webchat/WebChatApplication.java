package com.ufsc.webchat;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.ExternalHandler;
import com.ufsc.webchat.server.InternalHandler;
import com.ufsc.webchat.server.ManagerThread;
import com.ufsc.webchat.server.Server;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");

		EntityManagerProvider.init();

		ExternalHandler externalHandler = new ExternalHandler();
		InternalHandler internalHandler = new InternalHandler();
		ManagerThread managerThread = new ManagerThread(externalHandler, internalHandler);

		externalHandler.setManagerThread(managerThread);
		internalHandler.setManagerThread(managerThread);

		Server webServer = new Server(externalHandler, internalHandler, managerThread);

		webServer.start(0);
	}
}
