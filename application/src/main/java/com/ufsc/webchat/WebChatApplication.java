package com.ufsc.webchat;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.ExternalHandler;
import com.ufsc.webchat.server.InternalHandler;
import com.ufsc.webchat.server.ManagerImpl;
import com.ufsc.webchat.server.Server;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");

		EntityManagerProvider.init();

		ExternalHandler externalHandler = new ExternalHandler();
		InternalHandler internalHandler = new InternalHandler();
		ManagerImpl managerImpl = new ManagerImpl(externalHandler, internalHandler);

		externalHandler.setManagerThread(managerImpl);
		internalHandler.setManagerThread(managerImpl);

		Server webServer = new Server(externalHandler, internalHandler, managerImpl);

		webServer.start(0);
	}
}
