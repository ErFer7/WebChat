package com.ufsc.webchat.server;

import java.net.URI;
import java.nio.channels.SocketChannel;

import org.snf4j.core.EndingAction;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;

public abstract class ClientHandler extends Handler {

	private final URI uri;
	private SocketChannel clientChannel;

	public ClientHandler(URI uri) {
		this.uri = uri;
	}

	public void sendPacket(Packet packet) {
		this.getSession().writenf(new TextFrame(packet.toString()));
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