package com.ufsc.webchat.server;

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
	private boolean packetFactoryHostSet;


	public ServerHandler(PacketFactory packetFactory) {
		this.sessions = new ConcurrentHashMap<String, IStreamSession>();
		this.packetFactory = packetFactory;
		this.packetFactoryHostSet = false;
	}

	public abstract void readPacket(Packet packet);

	public void sendPacket(String host, Packet packet) {
		IStreamSession session = sessions.get(host);
		session.writenf(new TextFrame(packet.toString()));
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		if (!this.packetFactoryHostSet) {
			this.packetFactory.setHost(session.getLocalAddress().toString());
			this.packetFactoryHostSet = true;
		}

		session.getAttributes().put("user-id", session.getRemoteAddress());
		sessions.put(session.getRemoteAddress().toString(), session);
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		super.sessionClosed(session);

		sessions.remove(session.getRemoteAddress().toString());
	}
}
