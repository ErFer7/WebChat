package com.ufsc.ine5418;

import com.ufsc.ine5418.config.NetworkConfiguration;
import com.ufsc.ine5418.server.GatewayServerHandler;
import com.ufsc.ine5418.server.Server;

public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		NetworkConfiguration config = new NetworkConfiguration();

		GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(config.getGatewayIdentifier(), config.getGatewayPassword());
		Server webServer = new Server(gatewayServerHandler);

		webServer.start(config.getGatewayPort());
	}
}
