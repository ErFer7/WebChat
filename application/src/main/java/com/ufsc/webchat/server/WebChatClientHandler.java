package com.ufsc.webchat.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;

public class WebChatClientHandler extends ClientHandler {

	private final String gatewayHost;
	private final int gatewayPort;
	private final Semaphore readySemaphore;

	public WebChatClientHandler(String gatewayHost, int gatewayPort) throws URISyntaxException, InterruptedException {
		super(new URI("ws://" + gatewayHost + ":" + gatewayPort));

		this.readySemaphore = new Semaphore(1);
		this.readySemaphore.acquire();
		this.gatewayHost = gatewayHost;
		this.gatewayPort = gatewayPort;
	}

	@Override
	public void readPacket(Packet packet) {
		((WebChatManager) this.getManager()).receiveConnectionResponse(packet);
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
		this.readySemaphore.release();
	}

	public String getGatewayHost() {
		return gatewayHost;
	}

	public int getGatewayPort() {
		return gatewayPort;
	}

	public Semaphore getReadySemaphore() {
		return readySemaphore;
	}
}
