package com.ufsc.webchat.server;

import static java.lang.System.getProperty;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

import org.json.JSONObject;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;

public class GatewayServerHandler extends ServerHandler {

	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final HashMap<String, String> applicationServersTokens;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;

	public GatewayServerHandler() {
		super(new PacketFactory(HostType.GATEWAY));

		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
		this.applicationServersTokens = new HashMap<>();
		this.secureRandom = new SecureRandom();
		this.encoder = Base64.getUrlEncoder();
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.APPLICATION) {
			this.processApplicationPackets(packet);
		} else if (packet.getHostType() == HostType.CLIENT) {
			this.processClientPackets(packet);
		}
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		super.sessionClosed(session);
		this.applicationServersTokens.remove(session.getRemoteAddress().toString());
	}

	// TODO: Refactor
	private void processApplicationPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST && packet.getPayloadType() == PayloadType.CONNECTION) {
			String host = packet.getHost();
			JSONObject payload = packet.getPayload();

			String identifier = payload.getString("identifier");
			String password = payload.getString("password");

			if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
				String token = this.generateToken();
				this.applicationServersTokens.put(host, token);
				this.sendPacket(host, this.packetFactory.createGatewayConnectionResponse(Status.OK, token));
			} else {
				this.sendPacket(host, this.packetFactory.createGatewayConnectionResponse(Status.ERROR, null));
			}
		}
	}

	private void processClientPackets(Packet packet) {
		// TODO: Implement
	}

	private boolean authenticate(String host, String token) {
		return this.applicationServersTokens.get(host).equals(token);
	}

	private String generateToken() {
		this.secureRandom.setSeed(System.currentTimeMillis());

		byte[] randomBytes = new byte[24];
		this.secureRandom.nextBytes(randomBytes);
		return this.encoder.encodeToString(randomBytes);
	}
}
