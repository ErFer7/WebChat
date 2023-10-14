package com.ufsc.webchat;

import com.ufsc.webchat.config.NetworkConfiguration;
import com.ufsc.webchat.server.GatewayServerHandler;
import com.ufsc.webchat.server.Server;

public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		NetworkConfiguration config = new NetworkConfiguration();

		GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(config.getGatewayIdentifier(), config.getGatewayPassword());
		Server webServer = new Server(gatewayServerHandler);

		webServer.start(config.getGatewayPort());
	}
}
