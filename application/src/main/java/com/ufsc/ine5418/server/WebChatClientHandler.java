package com.ufsc.ine5418.server;

import java.net.URI;
import java.net.URISyntaxException;

import com.ufsc.ine5418.protocol.Packet;

public class WebChatClientHandler extends ClientHandler {

	private final String gatewayHost;
	private final int gatewayPort;

	public WebChatClientHandler(String gatewayHost, int gatewayPort) throws URISyntaxException {
		super(new URI("ws://" + gatewayHost + ":" + gatewayPort));

		this.gatewayHost = gatewayHost;
		this.gatewayPort = gatewayPort;
	}

	@Override
	public void readPacket(Packet packet) {
		// TODO: Implement
	}

	public String getGatewayHost() {
		return gatewayHost;
	}

	public int getGatewayPort() {
		return gatewayPort;
	}
}
