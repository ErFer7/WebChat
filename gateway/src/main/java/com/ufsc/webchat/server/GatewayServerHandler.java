package com.ufsc.webchat.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

import com.ufsc.webchat.utils.Logger;
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
	private final HashMap<String, String> tempClientUserHost;
	private final HashMap<String, String> clientApplicationMap;
	private final HashMap<String, Integer> appServersConnectionsCount;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;

	public GatewayServerHandler(String gatewayIdentifier, String gatewayPassword) {
		super(new PacketFactory(HostType.GATEWAY));;

		this.gatewayIdentifier = gatewayIdentifier;
		this.gatewayPassword = gatewayPassword;
		this.applicationServersTokens = new HashMap<>();
		this.clientApplicationMap = new HashMap<>();  // maps user ids to Application Servers
		this.tempClientUserHost = new HashMap<>();  // temporary maps user id to host IP
		this.appServersConnectionsCount = new HashMap<>();
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
				this.appServersConnectionsCount.put(host, 0);
				status = Status.OK;
			} else {
				status = Status.ERROR;
			}

			this.sendPacket(host, packetFactory.createGatewayConnectionResponse(status, this.generateToken()));
		} else if (packet.getOperationType() == OperationType.RESPONSE && packet.getPayloadType() == PayloadType.ROUTING) {
			String appHost = packet.getHost();
			JSONObject payload = packet.getPayload();

			String userId = payload.getString("userId");

			Status status = null;
			String token = null;

			String userHost = this.tempClientUserHost.remove(userId);

			if (packet.getStatus() == Status.OK) {
				Integer count = this.appServersConnectionsCount.get(appHost) + 1;
				this.appServersConnectionsCount.put(appHost, count);
				this.clientApplicationMap.put(userId, appHost);
				status = Status.OK;
				token = payload.getString("token");
			} else {
				// try with other server?
				Logger.log(this.getClass().getSimpleName(), "Application routing failed");
				status = Status.ERROR;
			}

			this.sendPacket(userHost, packetFactory.createRoutingResponse(status, userId, token));
		}
	}

	private void processClientPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST && packet.getPayloadType() == PayloadType.ROUTING) {
			String client = packet.getHost();
			JSONObject payload = packet.getPayload();

			String identifier = payload.getString("identifier");
			String password = payload.getString("password");

			String token = null;

			// TODO: get id from database, user authentication
			token = this.generateToken();
			this.tempClientUserHost.put(identifier, client);

			String server = chooseServer();

			this.sendPacket(server, packetFactory.createRoutingRequest(identifier, token));
		}
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

	private String chooseServer() {  // dummy way to choose server
		Set<String> servers = this.appServersConnectionsCount.keySet();
		int smallestValue = Integer.MAX_VALUE;
		String lessUsedHost = "";

		for (String key : servers) {
			int currentValue = this.appServersConnectionsCount.get(key);
			if (currentValue < smallestValue) {
				smallestValue = currentValue;
				lessUsedHost = key;
			}
		}
		return  lessUsedHost;
	}
}
