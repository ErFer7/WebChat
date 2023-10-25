package com.ufsc.webchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ufsc.webchat.server.GatewayThread;

@SpringBootApplication
public class WebChatGateway {

	public static void main(String[] args) throws Exception {
		GatewayThread gateway = GatewayThread.getInstance();
		gateway.start();
		SpringApplication.run(WebChatGateway.class, args);
	}
}
