package com.ufsc.webchat.server;

import static java.lang.System.getProperty;
import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.JSONValidator;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.ApplicationContextMap;

public class ApplicationPacketProcessor {

	private final ServerHandler serverHandler;
	private final PacketFactory packetFactory;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final HashMap<Long, String> userIdApplicationIdMap = new HashMap<>();
	private final ApplicationContextMap applicationContextMap;
	private final HashMap<Long, String> userIdClientIdMap;
	private static final Logger logger = LoggerFactory.getLogger(ApplicationPacketProcessor.class);

	public ApplicationPacketProcessor(ServerHandler serverHandler,
			PacketFactory packetFactory,
			ApplicationContextMap applicationContextMap,
			HashMap<Long, String> userIdClientIdMap) {
		this.serverHandler = serverHandler;
		this.packetFactory = packetFactory;
		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
		this.applicationContextMap = applicationContextMap;
		this.userIdClientIdMap = userIdClientIdMap;
	}

	public void process(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			switch (packet.getPayloadType()) {
			case CONNECTION -> this.receiveApplicationConnectionRequest(packet);
			case DISCONNECTION -> this.receiveApplicationClientDisconnectionRequest(packet);
			case USER_APPLICATION_SERVER -> this.receiveApplicationUserApplicationServerRequest(packet);
			default -> logger.warn("Invalid payload type");
			}
		} else if (packet.getOperationType() == OperationType.RESPONSE && packet.getPayloadType() == PayloadType.ROUTING) {
			this.receiveApplicationClientRoutingResponse(packet);
		}
	}

	public void receiveApplicationConnectionRequest(Packet packet) {
		String id = packet.getId();
		JSONObject payload = packet.getPayload();
		if (isNull(payload)) {
			return;
		}

		var missingFields = JSONValidator.validate(payload, List.of("host", "identifier", "password", "externalPort"));
		if (!missingFields.isEmpty()) {
			return;
		}

		String identifier = payload.getString("identifier");
		String password = payload.getString("password");
		String host = payload.getString("host");
		int externalPort = payload.getInt("externalPort");

		if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
			String token = this.serverHandler.generateToken();
			this.serverHandler.associateIdToHost(host, id);
			this.applicationContextMap.add(id, token, host.split(":")[0] + ':' + externalPort);
			this.serverHandler.sendPacketById(id, this.packetFactory.createApplicationConnectionResponse(Status.OK, token));
		} else {
			this.serverHandler.sendPacketById(id, this.packetFactory.createErrorResponse(PayloadType.CONNECTION, "Erro na autenticação"));
		}
	}

	public void receiveApplicationClientRoutingResponse(Packet packet) {
		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		String userHost = this.userIdClientIdMap.remove(userId);

		// Verificação de autenticação do servidor de aplicação
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			// Avisa o cliente que o servidor de aplicação não está autenticado e a operação foi cancelada
			var responsePacket = this.packetFactory.createGatewayClientRoutingResponse(Status.ERROR, userId, null, null, null);
			this.serverHandler.sendPacketById(userHost, responsePacket);
			return;
		}

		if (packet.getStatus() == Status.OK) {
			this.applicationContextMap.incrementUserCount(applicationId);
			this.userIdApplicationIdMap.put(userId, applicationId);
			this.serverHandler.sendPacketById(userHost, this.packetFactory.createGatewayClientRoutingResponse(
					Status.OK,
					userId,
					payload.getString("token"),
					applicationId,
					this.applicationContextMap.getExternalHost(applicationId)
			));
		} else {
			// TODO: try with another server?
			logger.warn("Application routing failed");
			var responsePacket = this.packetFactory.createGatewayClientRoutingResponse(Status.ERROR, userId, null, null, null);
			this.serverHandler.sendPacketById(userHost, responsePacket);
		}
	}

	public void receiveApplicationClientDisconnectionRequest(Packet packet) {
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			return;
		}

		// TODO: Autenticar o servidor de aplicação

		String applicationId = packet.getId();
		Long userId = packet.getPayload().getLong("userId");

		this.userIdApplicationIdMap.remove(userId);
		this.applicationContextMap.decrementUserCount(applicationId);

		this.serverHandler.sendPacketById(applicationId, this.packetFactory.createGatewayClientDisconnectionResponse(userId));
	}

	public void receiveApplicationUserApplicationServerRequest(Packet packet) {
		// TODO: Autenticar o servidor de aplicação

		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();
		Long targetUserId = payload.getLong("targetUserId");
		String messageId = payload.getString("messageId");

		String targetApplicationHost = this.applicationContextMap.getExternalHost(this.userIdApplicationIdMap.get(targetUserId));

		Packet responsePacket;
		if (!isNull(targetApplicationHost)) {
			responsePacket = this.packetFactory.createApplicationUserApplicationServerResponse(messageId, targetApplicationHost);
		} else {
			responsePacket = this.packetFactory.createErrorResponse(PayloadType.USER_APPLICATION_SERVER,
					"Usuário não está conectado a nenhum servidor de aplicação.");
		}
		this.serverHandler.sendPacketById(applicationId, responsePacket);
	}

	private boolean authenticate(Packet packet, PayloadType payloadType) {
		String packetId = packet.getId();
		String token = packet.getToken();

		if (!this.applicationContextMap.getToken(packetId).equals(token)) {
			this.serverHandler.sendPacketById(packetId, this.packetFactory.createErrorResponse(payloadType, "Erro na autenticação"));
			return false;
		}

		return true;
	}

}
