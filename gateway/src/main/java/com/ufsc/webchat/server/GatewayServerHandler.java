package com.ufsc.webchat.server;

import static java.lang.System.getProperty;
import static java.util.Collections.min;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.database.service.Answer;
import com.ufsc.webchat.database.service.UserService;
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

	private final UserService userService = new UserService();
	private static final Logger logger = LoggerFactory.getLogger(GatewayServerHandler.class);

	public GatewayServerHandler() {
		super(new PacketFactory(HostType.GATEWAY));

		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
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

	private void processApplicationPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST && packet.getPayloadType() == PayloadType.CONNECTION) {
			this.processApplicationConnectionRequest(packet);
		} else if (packet.getOperationType() == OperationType.RESPONSE && packet.getPayloadType() == PayloadType.ROUTING) {
			this.processApplicationRoutingResponse(packet);
		}
	}

	private void processApplicationConnectionRequest(Packet packet) {
		String host = packet.getHost();
		JSONObject payload = packet.getPayload();

		String identifier = payload.getString("identifier");
		String password = payload.getString("password");

		if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
			String token = this.generateToken();
			this.applicationServersTokens.put(host, token);
			this.appServersConnectionsCount.put(host, 0);
			this.sendPacket(host, this.packetFactory.createGatewayConnectionResponse(Status.OK, token));
		} else {
			this.sendPacket(host, this.packetFactory.createGatewayConnectionResponse(Status.ERROR, null));
		}
	}

	private void processApplicationRoutingResponse(Packet packet) {
		String appHost = packet.getHost();
		JSONObject payload = packet.getPayload();

		String userId = payload.getString("userId");
		String userHost = this.tempClientUserHost.remove(userId);

		if (packet.getStatus() == Status.OK) {
			Integer count = this.appServersConnectionsCount.get(appHost) + 1;
			this.appServersConnectionsCount.put(appHost, count);
			this.clientApplicationMap.put(userId, appHost);
			this.sendPacket(userHost, this.packetFactory.createClientRoutingResponse(Status.OK, userId, payload.getString("token")));
		} else {
			// TODO: try with another server?
			logger.warn("Application routing failed");
			this.sendPacket(userHost, this.packetFactory.createClientRoutingResponse(Status.ERROR, userId, null));
		}
	}

	private void processClientPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			if (packet.getPayloadType() == PayloadType.ROUTING) {
				String client = packet.getHost();
				JSONObject payload = packet.getPayload();

				String identifier = payload.getString("identifier");
				String password = payload.getString("password");
				//				this.userService.login(payload);

				// TODO: get id from database, user authentication
				this.tempClientUserHost.put(identifier, client);

				String server = this.chooseServer();
				this.sendPacket(server, this.packetFactory.createClientRoutingRequest(identifier, this.generateToken()));
			} else if (packet.getPayloadType() == PayloadType.REGISTER_USER) {
				String client = packet.getHost();
				Answer answer = this.userService.register(packet.getPayload());

				this.sendPacket(client, this.packetFactory.createClientRegisterUserResponse(answer.status(), answer.message()));
			}
		}
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

	private String chooseServer() {
		Integer minimum = min(this.appServersConnectionsCount.values());

		for (Map.Entry<String, Integer> entry : this.appServersConnectionsCount.entrySet()) {
			if (Objects.equals(entry.getValue(), minimum)) {
				return entry.getKey();
			}
		}

		return null;
	}
}
