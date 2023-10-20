package com.ufsc.webchat.server;

import static java.lang.System.getProperty;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.database.service.UserService;
import com.ufsc.webchat.model.ServiceAnswer;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.ApplicationContextMap;

public class ServerHandler extends Handler {

	private final String id;
	private final PacketFactory packetFactory;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final ApplicationContextMap applicationContextMap;
	private final HashMap<Long, String> userIdClientIdMap;
	private final HashMap<Long, String> userIdApplicationIdMap;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;
	private final UserService userService = new UserService();
	private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	public ServerHandler() {
		this.id = randomUUID().toString();
		this.packetFactory = new PacketFactory(this.id, HostType.GATEWAY);
		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
		this.applicationContextMap = new ApplicationContextMap();
		this.userIdApplicationIdMap = new HashMap<>();  // maps user ids to Application Servers
		this.userIdClientIdMap = new HashMap<>();  // temporary maps user id to client id
		this.secureRandom = new SecureRandom();
		this.encoder = Base64.getUrlEncoder();
	}

	@Override public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.APPLICATION) {
			this.processApplicationPackets(packet);
		} else if (packet.getHostType() == HostType.CLIENT) {
			this.processClientPackets(packet);
		}
	}

	@Override protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		this.sendPacketBySession(session, this.packetFactory.createHandshakeInfo(session.getRemoteAddress().toString()));
	}

	@Override protected void sessionClosed(IWebSocketSession session) {
		this.applicationContextMap.remove(sessions.getIdByName(session.getName()));

		super.sessionClosed(session);
	}

	private boolean authenticate(Packet packet, PayloadType payloadType) {
		String id = packet.getId();
		String token = packet.getToken();

		if (!this.applicationContextMap.getToken(id).equals(token)) {
			this.sendPacketById(id, this.packetFactory.createAuthenticationErrorResponse(payloadType));
			return false;
		}

		return true;
	}

	private String generateToken() {
		this.secureRandom.setSeed(System.currentTimeMillis());

		byte[] randomBytes = new byte[24];
		this.secureRandom.nextBytes(randomBytes);
		return this.encoder.encodeToString(randomBytes);
	}

	private void processApplicationPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			if (packet.getPayloadType() == PayloadType.CONNECTION) {
				this.receiveApplicationConnectionRequest(packet);
			} else if (packet.getPayloadType() == PayloadType.DISCONNECTION) {
				this.receiveApplicationClientDisconnectionRequest(packet);
			}
		} else if (packet.getOperationType() == OperationType.RESPONSE && packet.getPayloadType() == PayloadType.ROUTING) {
			this.receiveApplicationClientRoutingResponse(packet);
		}
	}

	private void processClientPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			if (packet.getPayloadType() == PayloadType.ROUTING) {
				this.receiveClientRoutingRequest(packet);
			} else if (packet.getPayloadType() == PayloadType.USER_CREATION) {
				this.receiveClientRegisterRequest(packet);
			}
		}
	}

	private void receiveApplicationConnectionRequest(Packet packet) {
		String id = packet.getId();
		JSONObject payload = packet.getPayload();

		String identifier = payload.getString("identifier");
		String password = payload.getString("password");
		String host = payload.getString("host");
		int externalPort = payload.getInt("externalPort");

		if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
			String token = this.generateToken();
			this.associateIdToHost(host, id);
			this.applicationContextMap.add(id, token, host.split(":")[0] + ':' + externalPort);
			this.sendPacketById(id, this.packetFactory.createApplicationConnectionResponse(Status.OK, token));
		} else {
			this.sendPacketById(id, this.packetFactory.createApplicationConnectionResponse(Status.ERROR, null));
		}
	}

	private void receiveApplicationClientRoutingResponse(Packet packet) {
		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		String userHost = this.userIdClientIdMap.remove(userId);

		// Verificação de autenticação do servidor de aplicação
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			// Avisa o cliente que o servidor de aplicação não está autenticado e a operação foi cancelada
			this.sendPacketById(userHost, this.packetFactory.createGatewayClientRoutingResponse(Status.ERROR, userId, null, null, null));
			return;
		}

		if (packet.getStatus() == Status.OK) {
			this.applicationContextMap.incrementUserCount(applicationId);
			this.userIdApplicationIdMap.put(userId, applicationId);
			this.sendPacketById(userHost, this.packetFactory.createGatewayClientRoutingResponse(
					Status.OK,
					userId,
					payload.getString("token"),
					applicationId,
					this.applicationContextMap.getExternalHost(applicationId)
			));
		} else {
			// TODO: try with another server?
			logger.warn("Application routing failed");
			this.sendPacketById(userHost, this.packetFactory.createGatewayClientRoutingResponse(Status.ERROR, userId, null, null, null));
		}
	}

	private void receiveApplicationClientDisconnectionRequest(Packet packet) {
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			return;
		}

		// TODO: Autenticar o servidor de aplicação

		String applicationId = packet.getId();
		Long userId = packet.getPayload().getLong("userId");

		this.userIdApplicationIdMap.remove(userId);
		this.applicationContextMap.decrementUserCount(applicationId);

		this.sendPacketById(applicationId, this.packetFactory.createGatewayClientDisconnectionResponse(userId));
	}

	private void receiveClientRoutingRequest(Packet packet) {
		String clientId = packet.getId();

		JSONObject payload = packet.getPayload();

		String host = payload.getString("host");

		this.associateIdToHost(host, clientId);

		Long userId = this.userService.login(packet.getPayload());
		if (isNull(userId)) {
			this.sendPacketById(clientId, this.packetFactory.createClientLoginErrorResponse());
		} else {
			this.userIdClientIdMap.put(userId, clientId);
			String serverId = this.applicationContextMap.chooseLeastLoadedApplication();
			this.sendPacketById(serverId, this.packetFactory.createClientRoutingRequest(userId, this.generateToken()));
		}
	}

	private void receiveClientRegisterRequest(Packet packet) {
		String clientId = packet.getId();

		JSONObject payload = packet.getPayload();

		String host = payload.getString("host");

		this.associateIdToHost(host, clientId);

		ServiceAnswer serviceAnswer = this.userService.register(packet.getPayload());
		this.sendPacketById(clientId, this.packetFactory.createClientRegisterUserResponse(serviceAnswer.status(), serviceAnswer.message()));
	}
}
