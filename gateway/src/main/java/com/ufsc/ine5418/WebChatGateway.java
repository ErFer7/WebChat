package com.ufsc.ine5418;

import com.ufsc.ine5418.config.NetworkConfiguration;
import com.ufsc.ine5418.server.GatewayManager;
import com.ufsc.ine5418.server.GatewayServerHandler;
import com.ufsc.ine5418.server.Server;

public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		NetworkConfiguration config = new NetworkConfiguration();

		GatewayServerHandler gatewayServerHandler = new GatewayServerHandler();
		GatewayManager webChatManager = new GatewayManager();
		Server webServer = new Server(gatewayServerHandler, webChatManager);

		webServer.start(config.getGatewayPort());
	}
}
