package com.ufsc.webchat.server;

import static java.util.UUID.randomUUID;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.utils.ApplicationContextMap;

public class ServerHandler extends Handler {

	private final String id;
	private final PacketFactory packetFactory;
	private final ApplicationContextMap applicationContextMap;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;
	private final ApplicationPacketProcessor applicationPacketProcessor;
	private final ClientPacketProcessor clientPacketProcessor;

	public ServerHandler() {
		this.id = randomUUID().toString();
		this.packetFactory = new PacketFactory(this.id, HostType.GATEWAY);
		this.applicationContextMap = new ApplicationContextMap();
		this.secureRandom = new SecureRandom();
		this.encoder = Base64.getUrlEncoder();
		HashMap<Long, String> userIdClientIdMap = new HashMap<>();
		this.applicationPacketProcessor = new ApplicationPacketProcessor(this, this.packetFactory, this.applicationContextMap, userIdClientIdMap);
		this.clientPacketProcessor = new ClientPacketProcessor(this, this.packetFactory, this.applicationContextMap, userIdClientIdMap);
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.APPLICATION) {
			this.applicationPacketProcessor.process(packet);
		} else if (packet.getHostType() == HostType.CLIENT) {
			this.clientPacketProcessor.process(packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
		this.sendPacketBySession(session, this.packetFactory.createHandshakeInfo(session.getRemoteAddress().toString()));
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		this.applicationContextMap.remove(this.sessions.getIdByName(session.getName()));
		super.sessionClosed(session);
	}

	public String generateToken() {
		this.secureRandom.setSeed(System.currentTimeMillis());

		byte[] randomBytes = new byte[24];
		this.secureRandom.nextBytes(randomBytes);
		return this.encoder.encodeToString(randomBytes);
	}

}
