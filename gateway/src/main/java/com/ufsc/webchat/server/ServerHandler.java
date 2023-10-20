package com.ufsc.webchat.server;

import static java.lang.System.getProperty;
import static java.util.Objects.isNull;

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

	private final PacketFactory packetFactory;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final ApplicationContextMap applicationContextMap;
	private final HashMap<Long, String> clientIdHostMap;
	private final HashMap<Long, String> clientIdApplicationMap;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;
	private final UserService userService = new UserService();
	private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	public ServerHandler() {
		this.packetFactory = new PacketFactory(HostType.GATEWAY);
		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
		this.applicationContextMap = new ApplicationContextMap();
		this.clientIdApplicationMap = new HashMap<>();  // maps user ids to Application Servers
		this.clientIdHostMap = new HashMap<>();  // temporary maps user id to host IP
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
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		if (isNull(this.packetFactory.getHost())) {
			this.packetFactory.setHost(session.getLocalAddress().toString());
		}
	}

	@Override
	protected void sessionClosed(IWebSocketSession session) {
		super.sessionClosed(session);
		this.applicationContextMap.remove(session.getRemoteAddress().toString());
	}

	private boolean authenticate(Packet packet, PayloadType payloadType) {
		String host = packet.getHost();
		String token = packet.getToken();

		if (!this.applicationContextMap.getToken(host).equals(token)) {
			this.sendPacket(host, this.packetFactory.createAuthenticationErrorResponse(payloadType));
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
		String host = packet.getHost();
		JSONObject payload = packet.getPayload();

		String identifier = payload.getString("identifier");
		String password = payload.getString("password");

		if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
			String token = this.generateToken();
			this.applicationContextMap.add(host, token);
			this.sendPacket(host, this.packetFactory.createApplicationConnectionResponse(Status.OK, token));
		} else {
			this.sendPacket(host, this.packetFactory.createApplicationConnectionResponse(Status.ERROR, null));
		}
	}

	private void receiveApplicationClientRoutingResponse(Packet packet) {
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		String appAddr = packet.getHost();
		String userHost = this.clientIdHostMap.remove(userId);

		// Verificação de autenticação do servidor de aplicação
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			// Avisa o cliente que o servidor de aplicação não está autenticado e a operação foi cancelada
			this.sendPacket(userHost, this.packetFactory.createClientRoutingResponse(Status.ERROR, userId, null));
			return;
		}

		if (packet.getStatus() == Status.OK) {
			this.applicationContextMap.incrementUserCount(appAddr);
			this.clientIdApplicationMap.put(userId, appAddr);
			this.sendPacket(userHost, this.packetFactory.createClientRoutingResponse(Status.OK, userId, payload.getString("token")));
		} else {
			// TODO: try with another server?
			logger.warn("Application routing failed");
			this.sendPacket(userHost, this.packetFactory.createClientRoutingResponse(Status.ERROR, userId, null));
		}
	}

	private void receiveApplicationClientDisconnectionRequest(Packet packet) {
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			return;
		}

		String appAddr = packet.getHost();
		Long userId = packet.getPayload().getLong("userId");

		this.clientIdApplicationMap.remove(userId);
		this.applicationContextMap.decrementUserCount(appAddr);

		this.sendPacket(appAddr, this.packetFactory.createGatewayClientDisconnectionResponse(userId));
	}

	private void receiveClientRoutingRequest(Packet packet) {
		String clientAddr = packet.getHost();

		Long userId = this.userService.login(packet.getPayload());
		if (isNull(userId)) {
			this.sendPacket(clientAddr, this.packetFactory.createClientLoginErrorResponse());
		} else {
			this.clientIdHostMap.put(userId, clientAddr);
			String server = this.applicationContextMap.chooseLeastLoadedApplication();
			this.sendPacket(server, this.packetFactory.createClientRoutingRequest(userId, this.generateToken()));
		}
	}

	private void receiveClientRegisterRequest(Packet packet) {
		String clientAddr = packet.getHost();

		ServiceAnswer serviceAnswer = this.userService.register(packet.getPayload());
		this.sendPacket(clientAddr, this.packetFactory.createClientRegisterUserResponse(serviceAnswer.status(), serviceAnswer.message()));
	}
}
