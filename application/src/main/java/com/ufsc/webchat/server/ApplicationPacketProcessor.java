package com.ufsc.webchat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.UserContextMap;

public class ApplicationPacketProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationPacketProcessor.class);
	private final ExternalHandler externalHandler;
	private final PacketFactory packetFactory;
	private final UserContextMap userContextMap;

	public ApplicationPacketProcessor(ExternalHandler externalHandler, PacketFactory packetFactory, UserContextMap userContextMap) {
		this.externalHandler = externalHandler;
		this.packetFactory = packetFactory;
		this.userContextMap = userContextMap;
	}

	public void process(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveApplicationConnectionResponse(packet);
		case MESSAGE -> this.receiveApplicationMessageRedirection(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	private void receiveApplicationConnectionResponse(Packet packet) {
		// TODO: Implementar o fluxo de resposta de conexão de aplicações
	}

	private void receiveApplicationMessageRedirection(Packet packet) {
		Long targetUserId = packet.getPayload().getLong("targetId");

		String targetUserClientId = this.userContextMap.getClientId(targetUserId);

		this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createApplicationMessageResponse(Status.OK, packet.getPayload()));
	}

}
