package com.ufsc.webchat.websocket;

import static java.lang.System.getProperty;
import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.model.RoutingResponseDto;
import com.ufsc.webchat.protocol.JSONValidator;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.websocket.utils.ApplicationContextMap;

public class ApplicationPacketProcessor {

	private final ServerHandler serverHandler;
	private final PacketFactory packetFactory;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final HashMap<Long, String> userIdApplicationIdMap;
	private final ApplicationContextMap applicationContextMap;
	private final HashMap<Long, String> userIdClientIdMap;
	private static final Logger logger = LoggerFactory.getLogger(ApplicationPacketProcessor.class);

	public ApplicationPacketProcessor(ServerHandler serverHandler,
			PacketFactory packetFactory,
			HashMap<Long, String> userIdApplicationIdMap,
			ApplicationContextMap applicationContextMap,
			HashMap<Long, String> userIdClientIdMap) {
		this.serverHandler = serverHandler;
		this.packetFactory = packetFactory;
		this.gatewayIdentifier = getProperty("gatewayIdentifier");
		this.gatewayPassword = getProperty("gatewayPassword");
		this.userIdApplicationIdMap = userIdApplicationIdMap;
		this.applicationContextMap = applicationContextMap;
		this.userIdClientIdMap = userIdClientIdMap;
	}

	public void process(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			switch (packet.getPayloadType()) {
			case APPLICATION_CONNECTION -> this.receiveApplicationConnectionRequest(packet);
			case CLIENT_CONNECTION -> this.receiveApplicationClientConnectionRequest(packet);
			case DISCONNECTION -> this.receiveApplicationClientDisconnectionRequest(packet);
			case MESSAGE_FORWARDING -> this.receiveApplicationMessageForwardingRequest(packet);
			default -> logger.warn("Invalid payload type");
			}
		} else if (packet.getOperationType() == OperationType.RESPONSE && packet.getPayloadType() == PayloadType.ROUTING) {
			this.receiveApplicationClientRoutingResponse(packet);
		}
	}

	public void receiveApplicationConnectionRequest(Packet packet) {
		String id = packet.getId();
		JSONObject payload = packet.getPayload();

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
			JSONObject newPayload = new JSONObject();
			newPayload.put("token", token);
			this.serverHandler.sendPacketById(id, this.packetFactory.createOkResponse(PayloadType.APPLICATION_CONNECTION, newPayload));
		} else {
			this.serverHandler.sendPacketById(id, this.packetFactory.createErrorResponse(PayloadType.APPLICATION_CONNECTION, "Erro na autenticação"));
		}
	}

	public void receiveApplicationClientConnectionRequest(Packet packet) {
		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("clientId", "userId"));
		if (!missingFields.isEmpty()) {
			this.serverHandler.sendPacketById(applicationId, this.packetFactory.createErrorResponse(PayloadType.CLIENT_CONNECTION, "Payload inválido"));
			return;
		}

		if (!this.authenticate(packet, PayloadType.CLIENT_CONNECTION)) {
			this.serverHandler.sendPacketById(applicationId, this.packetFactory.createErrorResponse(PayloadType.CLIENT_CONNECTION, "Aplicação não autenticada"));
			return;
		}

		this.userIdApplicationIdMap.put(payload.getLong("userId"), applicationId);
		this.applicationContextMap.incrementUserCount(applicationId);
		this.serverHandler.sendPacketById(applicationId, this.packetFactory.createOkResponse(PayloadType.CLIENT_CONNECTION, payload));
	}

	public void receiveApplicationClientRoutingResponse(Packet packet) {
		JSONObject payload = packet.getPayload();
		var missingFields = JSONValidator.validate(payload, List.of("userId", "token"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			return; // Request do client nunca seria completa, botar um TIMEOUT lá?
		}
		String applicationId = packet.getId();
		Long userId = payload.getLong("userId");
		String clientId = this.userIdClientIdMap.remove(userId);

		// Verificação de autenticação do servidor de aplicação
		if (!this.authenticate(packet, PayloadType.ROUTING)) {
			this.serverHandler.completeClientLoginRequest(clientId, new RoutingResponseDto(Status.ERROR));
			return;
		}

		if (packet.getStatus() == Status.OK) {
			this.serverHandler.completeClientLoginRequest(clientId, new RoutingResponseDto(
					Status.OK,
					payload.getString("token"),
					userId,
					this.applicationContextMap.getExternalHost(applicationId)));
		} else {
			logger.warn("Application routing failed");
			this.serverHandler.completeClientLoginRequest(clientId, new RoutingResponseDto(Status.ERROR));
		}
	}

	public void receiveApplicationClientDisconnectionRequest(Packet packet) {
		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			this.serverHandler.sendPacketById(applicationId, this.packetFactory.createErrorResponse(PayloadType.DISCONNECTION, "Payload inválido"));
			return;
		}

		if (!this.authenticate(packet, PayloadType.DISCONNECTION)) {
			return;
		}

		Long userId = packet.getPayload().getLong("userId");

		this.userIdApplicationIdMap.remove(userId);
		this.applicationContextMap.decrementUserCount(applicationId);

		this.serverHandler.sendPacketById(applicationId, this.packetFactory.createOkResponse(PayloadType.DISCONNECTION, "Desconexão realizada"));
	}

	public void receiveApplicationMessageForwardingRequest(Packet packet) {
		String applicationId = packet.getId();
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("targetUserId", "message", "chatId", "userId"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			this.serverHandler.sendPacketById(applicationId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE, "Payload inválido"));
			return;
		}

		if (!this.authenticate(packet, PayloadType.MESSAGE)) {
			return;
		}

		Long targetUserId = payload.getLong("targetUserId");
		String targetApplicationId = this.userIdApplicationIdMap.get(targetUserId);

		Packet responsePacket;
		if (!isNull(targetApplicationId)) {
			responsePacket = this.packetFactory.createOkResponse(PayloadType.MESSAGE, "Mensagem encaminhada");
			this.serverHandler.sendPacketById(targetApplicationId, this.packetFactory.createRequest(PayloadType.MESSAGE_FORWARDING, payload));
		} else {
			responsePacket = this.packetFactory.createErrorResponse(PayloadType.MESSAGE,
					"Usuário não está conectado a nenhum servidor de aplicação");
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
