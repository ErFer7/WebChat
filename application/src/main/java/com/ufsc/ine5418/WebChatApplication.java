package com.ufsc.ine5418;

import com.ufsc.ine5418.config.NetworkConfiguration;
import com.ufsc.ine5418.server.Server;
import com.ufsc.ine5418.server.WebChatClientHandler;
import com.ufsc.ine5418.server.WebChatManager;
import com.ufsc.ine5418.server.WebChatServerHandler;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		NetworkConfiguration config = new NetworkConfiguration();

		WebChatServerHandler webChatHandler = new WebChatServerHandler();
		WebChatClientHandler webChatClientHandler = new WebChatClientHandler(config.getGatewayHost(), config.getGatewayPort());
		WebChatManager webChatManager = new WebChatManager(webChatHandler, webChatClientHandler);
		Server webServer = new Server(webChatHandler, webChatClientHandler, webChatManager);

		webServer.start(config.getApplicationPort());
	}
}
