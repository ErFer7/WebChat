package com.ufsc.ine5418;

import com.ufsc.ine5418.server.WebChatHandler;
import com.ufsc.ine5418.server.WebServer;

public class WebChatApplication {

	public static void main(String[] args) throws Exception {
		WebChatHandler webChatHandler = new WebChatHandler();
		WebServer webServer = new WebServer(webChatHandler);

		webServer.start(8080);
	}
}
