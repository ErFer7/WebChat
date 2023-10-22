package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import java.util.HashMap;

import org.json.JSONObject;

import com.ufsc.webchat.database.service.UserService;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.utils.ApplicationContextMap;

public class ClientPacketProcessor {

	private final ServerHandler serverHandler;
	private final UserService userService = new UserService();
	private final PacketFactory packetFactory;
	private final ApplicationContextMap applicationContextMap;
	private final HashMap<Long, String> userIdClientIdMap;

	public ClientPacketProcessor(ServerHandler serverHandler,
			PacketFactory packetFactory,
			ApplicationContextMap applicationContextMap,
			HashMap<Long, String> userIdClientIdMap) {
		this.serverHandler = serverHandler;
		this.packetFactory = packetFactory;
		this.applicationContextMap = applicationContextMap;
		this.userIdClientIdMap = userIdClientIdMap;
	}

	public void process(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST) {
			if (packet.getPayloadType() == PayloadType.ROUTING) {
				this.receiveClientRoutingRequest(packet);
			} else if (packet.getPayloadType() == PayloadType.USER_CREATION) {
				this.receiveClientRegisterRequest(packet);
			}
		}
	}

	private void receiveClientRoutingRequest(Packet packet) {
		String clientId = packet.getId();
		JSONObject payload = packet.getPayload();
		String host = payload.getString("host");

		this.serverHandler.associateIdToHost(host, clientId);

		Long userId = this.userService.login(packet.getPayload());

		if (isNull(userId)) {
			var responsePacket = this.packetFactory.createErrorResponse(PayloadType.ROUTING, "Falha no login, usuário não encontrado ou senha incorreta.");
			this.serverHandler.sendPacketById(clientId, responsePacket);
		} else {
			this.userIdClientIdMap.put(userId, clientId);
			String serverId = this.applicationContextMap.chooseLeastLoadedApplication();
			var responsePacket = this.packetFactory.createClientRoutingRequest(userId, this.serverHandler.generateToken());
			this.serverHandler.sendPacketById(serverId, responsePacket);
		}
	}

	private void receiveClientRegisterRequest(Packet packet) {
		String clientId = packet.getId();
		JSONObject payload = packet.getPayload();
		String host = payload.getString("host");

		this.serverHandler.associateIdToHost(host, clientId);

		ServiceResponse serviceAnswer = this.userService.register(packet.getPayload());
		this.serverHandler.sendPacketById(clientId, this.packetFactory.createClientRegisterUserResponse(serviceAnswer.status(), serviceAnswer.message()));
	}

}