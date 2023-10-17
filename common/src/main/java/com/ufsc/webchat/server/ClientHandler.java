package com.ufsc.webchat.server;

import java.net.URI;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.EndingAction;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;

public abstract class ClientHandler extends Handler {

	private final URI uri;
	private SocketChannel clientChannel;
	private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

	public ClientHandler(URI uri) {
		this.uri = uri;
	}

	public void sendPacket(Packet packet) {
		try {
			this.getSession().writenf(new TextFrame(packet.toString()));
		} catch (Exception e) {
			logger.error("Error while sending packet: {}", packet);
		}
	}

	@Override
	public IWebSocketSessionConfig getConfig() {
		DefaultWebSocketSessionConfig config = new DefaultWebSocketSessionConfig(this.uri);
		config.setEndingAction(EndingAction.STOP);

		return config;
	}

	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}

	public SocketChannel getClientChannel() {
		return clientChannel;
	}
}
