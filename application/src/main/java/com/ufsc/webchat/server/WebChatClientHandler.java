package com.ufsc.webchat.server;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.enums.PayloadType;

public class WebChatClientHandler extends ClientHandler {

	private final String gatewayHost;
	private final int gatewayPort;
	private final Semaphore readySemaphore;

	public WebChatClientHandler() throws URISyntaxException, InterruptedException {
		super(new URI("ws://" + getProperty("gatewayHost") + ":" + parseInt(getProperty("gatewayPort"))));

		this.readySemaphore = new Semaphore(1);
		this.readySemaphore.acquire();
		this.gatewayHost = getProperty("gatewayHost");
		this.gatewayPort = parseInt(getProperty("gatewayPort"));
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getPayloadType() == PayloadType.CONNECTION) {
			((WebChatManagerThread) this.getManager()).receiveConnectionResponse(packet);
		} else if (packet.getPayloadType() == PayloadType.ROUTING) {
			((WebChatManagerThread) this.getManager()).receiveRoutingRequest(packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
		this.readySemaphore.release();
	}

	public String getGatewayHost() {
		return this.gatewayHost;
	}

	public int getGatewayPort() {
		return this.gatewayPort;
	}

	public Semaphore getReadySemaphore() {
		return this.readySemaphore;
	}
}
