package com.ufsc.webchat.server;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

import org.snf4j.core.EndingAction;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.IWebSocketSessionConfig;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.enums.HostType;

public class InternalHandler extends Handler {

	private final URI uri;
	private final String gatewayHost;
	private final int gatewayPort;
	private final Semaphore readyLock;

	public InternalHandler() throws URISyntaxException, InterruptedException {
		this.uri = new URI("ws://" + getProperty("gatewayHost") + ":" + parseInt(getProperty("gatewayPort")));

		this.gatewayHost = getProperty("gatewayHost");
		this.gatewayPort = parseInt(getProperty("gatewayPort"));
		this.readyLock = new Semaphore(1);
		this.readyLock.acquire();
	}

	@Override
	public IWebSocketSessionConfig getConfig() {
		DefaultWebSocketSessionConfig config = new DefaultWebSocketSessionConfig(this.uri);
		config.setEndingAction(EndingAction.STOP);

		return config;
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.GATEWAY) {
			((ManagerImpl) this.managerThread).processGatewayPackets(packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
		this.readyLock.release();
	}

	public String getGatewayHost() {
		return this.gatewayHost;
	}

	public int getGatewayPort() {
		return this.gatewayPort;
	}

	public Semaphore getReadyLock() {
		return this.readyLock;
	}

	@Override
	public SocketChannel getInternalChannel() {
		return (SocketChannel) this.internalChannel;
	}
}
