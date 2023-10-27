package com.ufsc.webchat.websocket;

import static java.lang.Integer.parseInt;

import org.slf4j.Logger;

import com.ufsc.webchat.config.PropertyLoader;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.server.Server;

public class GatewayThread extends Thread {

	private static GatewayThread INSTANCE;
	private final ServerHandler serverHandler;
	private final Server server;
	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(GatewayThread.class);

	public GatewayThread() throws Exception {
		PropertyLoader.loadAndSetSystemProperties("config/network.properties");
		PropertyLoader.loadAndSetSystemProperties("config/application.properties");

		EntityManagerProvider.init();

		this.serverHandler = new ServerHandler();
		this.server = new Server(this.serverHandler);
	}

	public static GatewayThread getInstance() {
		try {
			if (INSTANCE == null) {
				INSTANCE = new GatewayThread();
			}
		} catch (Exception exception) {
			logger.error("Error while creating gateway thread", exception);
		}

		return INSTANCE;
	}

	public ServerHandler getServerHandler() {
		return this.serverHandler;
	}

	@Override
	public void run() {
		try {
			this.server.start(parseInt(System.getProperty("gatewayPort")));
		} catch (Exception exception) {
			logger.error("Error while starting gateway server", exception);
		}
	}

}
