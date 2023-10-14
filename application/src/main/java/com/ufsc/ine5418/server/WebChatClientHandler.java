package com.ufsc.ine5418.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.utils.Logger;

public class WebChatClientHandler extends ClientHandler {

	private final String gatewayHost;
	private final int gatewayPort;
	private Semaphore readySemaphore;

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
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		if (Objects.requireNonNull(event) == SessionEvent.READY) {
			Logger.log(this.getClass().getSimpleName(), "Session ready: " + session.getLocalAddress());
			this.readySemaphore.release();
		}
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
