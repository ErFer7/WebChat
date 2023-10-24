package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.JSONValidator;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.SharedString;
import com.ufsc.webchat.utils.UserContextMap;

public class GatewayPacketProcessor {

	private static final Logger logger = LoggerFactory.getLogger(GatewayPacketProcessor.class);
	private final ExternalHandler externalHandler;
	private final InternalHandler internalHandler;
	private final PacketFactory packetFactory;
	private final String gatewayHost;
	private SharedString gatewayId;
	private final int gatewayPort;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final UserContextMap userContextMap;

	public GatewayPacketProcessor(ExternalHandler externalHandler,
			InternalHandler internalHandler,
			PacketFactory packetFactory,
			UserContextMap userContextMap,
			SharedString gatewayId) {
		this.externalHandler = externalHandler;
		this.internalHandler = internalHandler;
		this.packetFactory = packetFactory;
		this.gatewayHost = this.internalHandler.getGatewayHost();
		this.gatewayPort = this.internalHandler.getGatewayPort();
		this.userContextMap = userContextMap;
		this.gatewayIdentifier = System.getProperty("gatewayIdentifier");
		this.gatewayPassword = System.getProperty("gatewayPassword");
		this.gatewayId = gatewayId;
	}

	public void process(Packet packet) {
		switch (packet.getPayloadType()) {
		case HOST -> this.receiveGatewayHostInfo(packet);
		case CONNECTION -> this.receiveGatewayConnectionResponse(packet);
		case ROUTING -> this.receiveGatewayClientRoutingRequest(packet);
		case MESSAGE_FORWARDING -> this.receiveGatewayMessageForwarding(packet);
		case MESSAGE -> this.receiveGatewayMessageResponse(packet);
		case DISCONNECTION -> this.receiveGatewayClientDisconnectionResponse(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	public void receiveGatewayHostInfo(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("host"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			return;
		}

		String host = payload.getString("host");

		this.gatewayId.setString(packet.getId());
		this.internalHandler.associateIdToHost('/' + this.gatewayHost + ':' + this.gatewayPort, this.gatewayId.getString());

		logger.info("Sending connection request to gateway");

		int externalPort = this.externalHandler.getInternalChannel().socket().getLocalPort();
		Packet response = this.packetFactory.createGatewayConnectionRequest(this.gatewayIdentifier, this.gatewayPassword, host, externalPort);
		this.internalHandler.sendPacketById(this.gatewayId.getString(), response);
	}

	public void receiveGatewayConnectionResponse(Packet packet) {
		if (packet.getStatus() == Status.OK) {
			logger.info("Gateway authentication successful");

			JSONObject payload = packet.getPayload();

			var missingFields = JSONValidator.validate(payload, List.of("token"));
			if (!missingFields.isEmpty()) {
				logger.error("Invalid payload");
				return;
			}

			this.packetFactory.setToken(payload.getString("token"));
		} else {
			logger.warn("Gateway authentication failed");
		}
	}

	public void receiveGatewayClientRoutingRequest(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "token"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createErrorResponse(PayloadType.ROUTING, "Payload inválido"));
			return;
		}

		Long userId = payload.getLong("userId");
		String token = payload.getString("token");

		this.userContextMap.add(userId, token);

		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createApplicationClientRoutingResponse(Status.OK, userId, token));
	}

	private void receiveGatewayClientDisconnectionResponse(Packet packet) {
		JSONObject payload = packet.getPayload();
		Long userId = null;

		if (packet.getStatus() == Status.ERROR) {
			logger.error("Client disconnection failed: {}", payload.getString("message"));
		} else {
			var missingFields = JSONValidator.validate(payload, List.of("userId"));
			if (!missingFields.isEmpty()) {
				logger.error("Invalid payload");
			} else {
				userId = packet.getPayload().getLong("userId");
			}
		}

		this.userContextMap.remove(userId);
	}

	private void receiveGatewayMessageResponse(Packet packet) {
		String message = packet.getPayload().getString("message");

		if (packet.getStatus() == Status.OK) {
			logger.info(message);
		} else {
			logger.error(message);
		}
	}

	private void receiveGatewayMessageForwarding(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("targetUserId", "message", "chatId", "userId"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			return;
		}

		Long targetUserId = payload.getLong("targetUserId");
		String targetUserClientId = this.userContextMap.getClientId(targetUserId);

		if (!isNull(targetUserClientId)) {
			this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createApplicationMessageResponse(Status.OK, packet.getPayload()));
		} else {
			this.externalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createErrorResponse(PayloadType.MESSAGE, "Usuário não conectado"));
		}
	}
}
