package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.model.ChatDto;
import com.ufsc.webchat.database.service.ChatService;
import com.ufsc.webchat.database.service.MessageService;
import com.ufsc.webchat.database.service.UserService;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.SharedString;
import com.ufsc.webchat.utils.UserContextMap;

public class ClientPacketProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ClientPacketProcessor.class);
	private final ExternalHandler externalHandler;
	private final InternalHandler internalHandler;
	private final ChatService chatService = new ChatService();
	private final UserService userService = new UserService();
	private final MessageService messageService = new MessageService();
	private final PacketFactory packetFactory;
	private final SharedString gatewayId;
	private final UserContextMap userContextMap;
	private final HashMap<Long, String> externalUserIdApplicationIdMap;
	private final HashMap<Long, JSONObject> tempMessageMap;

	public ClientPacketProcessor(ExternalHandler externalHandler,
			InternalHandler internalHandler,
			PacketFactory packetFactory,
			UserContextMap userContextMap,
			HashMap<Long, String> externalUserIdApplicationIdMap,
			HashMap<Long, JSONObject> tempMessageMap,
			SharedString gatewayId) {
		this.externalHandler = externalHandler;
		this.internalHandler = internalHandler;
		this.packetFactory = packetFactory;
		this.userContextMap = userContextMap;
		this.externalUserIdApplicationIdMap = externalUserIdApplicationIdMap;
		this.tempMessageMap = tempMessageMap;
		this.gatewayId = gatewayId;
	}

	public void process(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveClientConnectionRequest(packet);
		case USER_LISTING -> this.receiveClientUserListingRequest(packet);
		case GROUP_CHAT_CREATION -> this.receiveClientGroupChatCreationRequest(packet);
		case CHAT_LISTING -> this.receiveClientChatListingRequest(packet);
		case GROUP_CHAT_ADDITION -> this.receiveClientGroupChatAdditionRequest(packet);
		case GET_USER_CHAT_ID -> this.receiveClientGetUserChatId(packet);
		case MESSAGE -> this.receiveClientMessage(packet);
		case MESSAGE_LISTING -> this.receiveClientMessageListing(packet);
		case DISCONNECTION -> this.receiveClientDisconnectionRequest(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	private boolean authenticateClient(Packet packet, PayloadType payloadType) {
		String clientId = packet.getId();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		if (!this.userContextMap.getToken(userId).equals(packet.getToken())) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(payloadType, "Erro na autenticação"));
			return false;
		}

		return true;
	}

	private void receiveClientConnectionRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.CONNECTION)) {
			return;
		}
		String clientId = packet.getId();

		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		String host = payload.getString("host");

		this.externalHandler.associateIdToHost(host, clientId);
		this.userContextMap.setUserClientId(userId, clientId);

		this.externalHandler.sendPacketById(clientId, this.packetFactory.createClientConnectionResponse(Status.OK));
	}

	private void receiveClientDisconnectionRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.DISCONNECTION)) {
			return;
		}
		String userId = packet.getPayload().getString("userId");

		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createApplicationClientDisconnectingRequest(userId));
	}

	private void receiveClientUserListingRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.USER_LISTING)) {
			return;
		}
		ServiceResponse serviceResponse = this.userService.loadUserDtoList();
		var responsePayload = new JSONObject(Map.of("users", serviceResponse.payload()));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.USER_LISTING, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveClientGroupChatCreationRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_CREATION)) {
			return;
		}

		ServiceResponse serviceResponse = this.chatService.saveChatGroup(packet.getPayload());
		var chatId = serviceResponse.payload();
		JSONObject responsePayload = isNull(chatId) ? null : new JSONObject(Map.of("chatId", chatId));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GROUP_CHAT_CREATION, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveClientChatListingRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.CHAT_LISTING)) {
			return;
		}
		List<ChatDto> chatDtoList = this.chatService.loadChatDtoListByUserId(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("chats", chatDtoList));
		var responsePacket = this.packetFactory.createGenericClientResponse(Status.OK, PayloadType.CHAT_LISTING, responsePayload, null);
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveClientGroupChatAdditionRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_ADDITION)) {
			return;
		}
		ServiceResponse serviceResponse = this.chatService.addToChatGroup(packet.getPayload());
		this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createGroupChatAdditionResponse(serviceResponse.status(), serviceResponse.message()));
	}

	private void receiveClientMessage(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.MESSAGE)) {
			return;
		}
		String clientId = packet.getId();
		JSONObject payload = packet.getPayload();  // TODO: Validar
		Long senderId = payload.getLong("userId");

		ServiceResponse messageServiceResponse = this.messageService.saveMessage(payload);
		if (messageServiceResponse.status() == Status.ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE, messageServiceResponse.message()));
			return;
		}
		ServiceResponse userServiceResponse = this.userService.loadUsersIdsFromChat(payload);
		if (userServiceResponse.status() == Status.ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE, userServiceResponse.message()));
			return;
		}
		//TODO: Avaliar unchecked cast
		this.broadCastClientMessage((List<Long>) userServiceResponse.payload(), senderId, (Long) messageServiceResponse.payload(), payload);
	}

	private void receiveClientMessageListing(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.MESSAGE_LISTING)) {
			return;
		}
		var messagesDto = this.messageService.loadMessages(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("messages", messagesDto));
		var packetResponse = this.packetFactory.createGenericClientResponse(Status.OK, PayloadType.MESSAGE_LISTING, responsePayload, null);
		this.externalHandler.sendPacketById(packet.getId(), packetResponse);
	}

	private void receiveClientGetUserChatId(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.MESSAGE)) {
			return;
		}
		ServiceResponse serviceResponse = this.chatService.loadOrSaveChatIdByUsers(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("chatId", serviceResponse.payload()));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GET_USER_CHAT_ID, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void broadCastClientMessage(List<Long> targetUsersIds, Long senderId, Long messageId, JSONObject payload) {
		for (Long targetUserId : targetUsersIds) {
			if (targetUserId.equals(senderId)) {
				continue;
			}

			String targetUserClientId = this.userContextMap.getClientId(targetUserId);

			if (targetUserClientId != null) {
				this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createApplicationMessageResponse(Status.OK, payload));
			} else {
				String applicationId = this.externalUserIdApplicationIdMap.get(targetUserId);
				payload.put("targetUserId", targetUserId);
				if (applicationId != null) {
					this.internalHandler.sendPacketById(applicationId, this.packetFactory.createApplicationMessageForwardingRequest(payload));
				} else {
					this.tempMessageMap.put(messageId, payload);
					this.internalHandler.sendPacketById(this.gatewayId.getString(),
							this.packetFactory.createApplicationUserApplicationServerRequest(messageId.toString(), targetUserId));
				}
			}
		}
	}

}
