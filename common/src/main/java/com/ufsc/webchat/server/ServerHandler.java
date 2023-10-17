package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.utils.OptionalKeyPairMap;

public abstract class ServerHandler extends Handler {

	protected final PacketFactory packetFactory;
	protected final OptionalKeyPairMap<String, String, IStreamSession> sessions;
	private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	public ServerHandler(PacketFactory packetFactory) {
		this.sessions = new OptionalKeyPairMap<>();
		this.packetFactory = packetFactory;
	}

	@Override public abstract void readPacket(Packet packet);

	public void sendPacket(String host, Packet packet) {
		try {
			IStreamSession session = this.sessions.getBySecondKey(host);
			session.writenf(new TextFrame(packet.toString()));
		} catch (Exception e) {
			logger.error("Error while sending packet: {}", packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		if (isNull(this.packetFactory.getHost())) {
			this.packetFactory.setHost(session.getLocalAddress().toString());
		}

		session.getAttributes().put("user-id", session.getRemoteAddress());
		this.sessions.put(session.getName(), session.getRemoteAddress().toString(), session);
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		super.sessionClosed(session);

		this.sessions.removeByFirstKey(session.getName());
	}
}
