package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;

public abstract class ServerHandler extends Handler {

	protected final PacketFactory packetFactory;
	protected final ConcurrentMap<String, IStreamSession> sessions;

	public ServerHandler(PacketFactory packetFactory) {
		this.sessions = new ConcurrentHashMap<>();
		this.packetFactory = packetFactory;
	}

	@Override public abstract void readPacket(Packet packet);

	public void sendPacket(String host, Packet packet) {
		IStreamSession session = this.sessions.get(host);
		session.writenf(new TextFrame(packet.toString()));
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		if (isNull(this.packetFactory.getHost())) {
			this.packetFactory.setHost(session.getLocalAddress().toString());
		}

		session.getAttributes().put("user-id", session.getRemoteAddress());
		this.sessions.put(session.getRemoteAddress().toString(), session);
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		super.sessionClosed(session);

		this.sessions.remove(session.getRemoteAddress().toString());
	}
}
