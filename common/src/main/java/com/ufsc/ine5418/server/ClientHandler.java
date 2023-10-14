package com.ufsc.ine5418.server;

import java.net.URI;

import org.snf4j.core.EndingAction;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.ine5418.protocol.Packet;

public abstract class ClientHandler extends Handler {

	private final URI uri;

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
}
