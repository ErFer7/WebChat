package com.ufsc.webchat.server;

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

	public GatewayServerHandler(String gatewayIdentifier, String gatewayPassword) {
		super(new PacketFactory(HostType.GATEWAY));;

		this.gatewayIdentifier = gatewayIdentifier;
		this.gatewayPassword = gatewayPassword;
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

			Status status = null;
			String token = null;

			if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
				token = this.generateToken();
				this.applicationServersTokens.put(host, token);
				status = Status.OK;
			} else {
				status = Status.ERROR;
			}

			this.sendPacket(host, packetFactory.createGatewayConnectionResponse(status, this.generateToken()));
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
		secureRandom.nextBytes(randomBytes);
		return this.encoder.encodeToString(randomBytes);
	}
}
