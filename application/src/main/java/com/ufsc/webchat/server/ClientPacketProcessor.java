package com.ufsc.webchat.server;

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.model.ChatDto;
import com.ufsc.webchat.database.model.MessageCreateDto;
import com.ufsc.webchat.database.service.ChatService;
import com.ufsc.webchat.database.service.MessageService;
import com.ufsc.webchat.database.service.UserService;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.protocol.JSONValidator;
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

	public ClientPacketProcessor(ExternalHandler externalHandler,
			InternalHandler internalHandler,
			PacketFactory packetFactory,
			UserContextMap userContextMap,
			SharedString gatewayId) {
		this.externalHandler = externalHandler;
		this.internalHandler = internalHandler;
		this.packetFactory = packetFactory;
		this.userContextMap = userContextMap;
		this.gatewayId = gatewayId;
	}

	public void process(Packet packet) {
		switch (packet.getPayloadType()) {
		case CLIENT_CONNECTION -> this.receiveClientConnectionRequest(packet);
		case USER_LISTING -> this.receiveClientUserListingRequest(packet);
		case GROUP_CHAT_CREATION -> {
			try {
				Object[] argument = { packet };
				RetryProcessor.executeWithRetry(this, this.getClass().getMethod("receiveClientGroupChatCreationRequest", Packet.class), argument);
			} catch (Exception e) {
				logger.error("Invalid function");
			}
		}
		case CHAT_LISTING -> this.receiveClientChatListingRequest(packet);
		case GROUP_CHAT_ADDITION -> {
			try {
				Object[] argument = { packet };
				RetryProcessor.executeWithRetry(this, this.getClass().getMethod("receiveClientGroupChatAdditionRequest", Packet.class), argument);
			} catch (Exception e) {
				logger.error("Invalid function");
			}
		}
		case GET_USER_CHAT_ID -> this.receiveClientGetUserChatId(packet);
		case MESSAGE -> {
			try {
				Object[] argument = { packet };
				RetryProcessor.executeWithRetry(this, this.getClass().getMethod("receiveClientMessage", Packet.class), argument);
			} catch (Exception e) {
				logger.error("Invalid function");
			}
		}
		case MESSAGE_LISTING -> this.receiveClientMessageListing(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

//	public void tryAgain(Packet packet) {
//		switch (packet.getPayloadType()) {
//		case GROUP_CHAT_CREATION -> this.retryClientGroupChatCreationRequest(packet);
//		case GROUP_CHAT_ADDITION -> this.retryClientGroupChatAdditionRequest(packet);
//		case MESSAGE -> this.retryClientMessage(packet);
//		default -> logger.warn("Unexpected packet type in retry: {}", packet.getPayloadType());
//		}
//	}

	public void receiveClientDisconnection(String clientId) {
		Long userId = this.userContextMap.getUserIdByClientId(clientId);
		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createApplicationClientDisconnectingRequest(userId));
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
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "host"));
		if (!missingFields.isEmpty()) {
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.CLIENT_CONNECTION)) {
			return;
		}

		String clientId = packet.getId();

		Long userId = payload.getLong("userId");
		String host = payload.getString("host");

		this.externalHandler.associateIdToHost(host, clientId);
		this.userContextMap.setUserClientId(userId, clientId);

		JSONObject newPayload = new JSONObject();
		newPayload.put("clientId", clientId);

		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createRequest(PayloadType.CLIENT_CONNECTION, newPayload));
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

	@Retry(maxAttempts = 3, delayMillis = 1000, onException = RetryException.class)
	public void receiveClientGroupChatCreationRequest(Packet packet) throws RetryException {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("groupName", "membersUsernames", "userId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.GROUP_CHAT_CREATION, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_CREATION)) {
			return;
		}

		ServiceResponse serviceResponse = this.chatService.saveChatGroup(packet.getPayload());
		var chatId = serviceResponse.payload();
		JSONObject responsePayload = isNull(chatId) ? null : new JSONObject(Map.of("chatId", chatId));

		if (serviceResponse.status().equals(Status.ERROR)) {
			throw new RetryException("Erro receiveClientGroupChatCreationRequest");
		}

		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GROUP_CHAT_CREATION, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveClientChatListingRequest(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.CHAT_LISTING, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.CHAT_LISTING)) {
			return;
		}
		List<ChatDto> chatDtoList = this.chatService.loadChatDtoListByUserId(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("chats", chatDtoList));
		var responsePacket = this.packetFactory.createGenericClientResponse(Status.OK, PayloadType.CHAT_LISTING, responsePayload, null);
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	@Retry(maxAttempts = 3, delayMillis = 1000, onException = RetryException.class)
	public void receiveClientGroupChatAdditionRequest(Packet packet) throws RetryException {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "chatId", "addedUserName"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.GROUP_CHAT_ADDITION, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_ADDITION)) {
			return;
		}
		ServiceResponse serviceResponse = this.chatService.addToChatGroup(packet.getPayload());

		if (serviceResponse.status().equals(Status.ERROR)) {
			throw new RetryException("Erro receiveClientGroupChatAdditionRequest");
		}

		this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createGroupChatAdditionResponse(serviceResponse.status(), serviceResponse.message()));
	}

	@Retry(maxAttempts = 2, delayMillis = 1000, onException = RetryException.class)
	public void receiveClientMessage(Packet packet) throws RetryException {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("message", "chatId", "userId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.MESSAGE, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.MESSAGE)) {
			return;
		}

		String clientId = packet.getId();
		Long senderId = payload.getLong("userId");

		ServiceResponse messageServiceResponse = this.messageService.saveMessage(payload);
		if (messageServiceResponse.status() == Status.ERROR) {
			throw new RetryException("Erro receiveClientMessage");
		} else if (messageServiceResponse.status() == Status.VALIDATION_ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE, messageServiceResponse.message()));
			return;
		}

		ServiceResponse userServiceResponse = this.userService.loadUsersIdsFromChat(payload);
		if (userServiceResponse.status() == Status.VALIDATION_ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE, userServiceResponse.message()));
			return;
		}
		MessageCreateDto messageCreateDto = (MessageCreateDto) messageServiceResponse.payload();
		payload.put("senderUsername", messageCreateDto.getSenderUsername());
		payload.put("sentAt", messageCreateDto.getSentAt());

		this.externalHandler.sendPacketById(clientId, this.packetFactory.createOkResponse(PayloadType.MESSAGE_FEEDBACK, "Mensagem enviada com sucesso"));
		this.broadCastClientMessage((List<Long>) userServiceResponse.payload(), senderId, payload);
	}

	private void receiveClientMessageListing(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "chatId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.MESSAGE_LISTING, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.MESSAGE_LISTING)) {
			return;
		}

		var serviceResponse = this.messageService.loadMessages(packet.getPayload());
		var messagesDto = serviceResponse.payload();
		var responsePayload = isNull(messagesDto) ? null : new JSONObject(Map.of("messages", messagesDto));
		var packetResponse = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.MESSAGE_LISTING, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), packetResponse);
	}

	private void receiveClientGetUserChatId(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "targetUsername"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.GET_USER_CHAT_ID, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.GET_USER_CHAT_ID)) {
			return;
		}

		ServiceResponse serviceResponse = this.chatService.loadOrSaveChatIdByUsers(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("chatId", serviceResponse.payload()));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GET_USER_CHAT_ID, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void broadCastClientMessage(List<Long> targetUsersIds, Long senderId, JSONObject payload) {
		for (Long targetUserId : targetUsersIds) {
			if (targetUserId.equals(senderId)) {
				continue;
			}

			String targetUserClientId = this.userContextMap.getClientId(targetUserId);

			if (targetUserClientId != null) {
				this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createApplicationMessageResponse(Status.OK, payload));
			} else {
				payload.put("targetUserId", targetUserId);
				this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createMessageForwarding(payload));
			}
		}
	}
}
