package com.ufsc.webchat.server;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;

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

	public InternalHandler() throws URISyntaxException, InterruptedException {
		this.uri = new URI("ws://" + getProperty("gatewayHost") + ":" + parseInt(getProperty("gatewayPort")));

		this.gatewayHost = getProperty("gatewayHost");
		this.gatewayPort = parseInt(getProperty("gatewayPort"));
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
			((ManagerThread) this.managerThread).processGatewayPackets(packet);
		} else if (packet.getHostType() == HostType.APPLICATION) {
			((ManagerThread) this.managerThread).processApplicationPackets(packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
	}

	public String getGatewayHost() {
		return this.gatewayHost;
	}

	public int getGatewayPort() {
		return this.gatewayPort;
	}

	@Override
	public SocketChannel getInternalChannel() {
		return (SocketChannel) this.internalChannel;
	}
}
