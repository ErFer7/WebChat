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
import com.ufsc.webchat.utils.retry.MaxAttemptCallable;
import com.ufsc.webchat.utils.retry.Retry;
import com.ufsc.webchat.utils.retry.RetryException;
import com.ufsc.webchat.utils.retry.RetryProcessor;

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
		MaxAttemptCallable maxAttemptCallable = arg -> this.whenMaxAttempts(packet, arg);
		Object[] argument = { packet };
		try {
			switch (packet.getPayloadType()) {
			case CLIENT_CONNECTION -> this.receiveConnectionRequest(packet);
			case USER_LISTING -> this.receiveUserListingRequest(packet);
			case GROUP_CHAT_CREATION -> RetryProcessor.execute(this, this.getClass().getMethod("receiveGroupChatCreationRequest", Packet.class), argument, maxAttemptCallable);
			case CHAT_LISTING -> this.receiveChatListingRequest(packet);
			case GROUP_CHAT_ADDITION -> RetryProcessor.execute(this, this.getClass().getMethod("receiveGroupChatAdditionRequest", Packet.class), argument, maxAttemptCallable);
			case GET_USER_CHAT_ID -> this.receiveGetUserChatId(packet);
			case MESSAGE -> RetryProcessor.execute(this, this.getClass().getMethod("receiveMessageRequest", Packet.class), argument, maxAttemptCallable);
			case MESSAGE_LISTING -> this.receiveMessageListing(packet);
			case CHAT_USERS_LISTING -> this.receiveChatUsersListing(packet);
			default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void receiveClientDisconnection(String clientId) {
		Long userId = this.userContextMap.getUserIdByClientId(clientId);
		JSONObject payload = new JSONObject(Map.of("userId", userId));

		this.userContextMap.removeClientId(userId);

		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createRequest(PayloadType.DISCONNECTION, payload));
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

	private void receiveConnectionRequest(Packet packet) {
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
		newPayload.put("userId", userId);
		newPayload.put("clientId", clientId);

		this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createRequest(PayloadType.CLIENT_CONNECTION, newPayload));
	}

	private void receiveUserListingRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.USER_LISTING)) {
			return;
		}
		ServiceResponse serviceResponse = this.userService.loadUserDtoList();
		var responsePayload = new JSONObject(Map.of("users", serviceResponse.payload()));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.USER_LISTING, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	@Retry(maxAttempts = 3, delayMillis = 1000, onException = RetryException.class)
	public void receiveGroupChatCreationRequest(Packet packet) throws RetryException {
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
			throw new RetryException(serviceResponse.message());
		}

		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GROUP_CHAT_CREATION, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveChatListingRequest(Packet packet) {
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
	public void receiveGroupChatAdditionRequest(Packet packet) throws RetryException {
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
			throw new RetryException(serviceResponse.message());
		}

		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GROUP_CHAT_ADDITION, null, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	@Retry(maxAttempts = 2, delayMillis = 1000, onException = RetryException.class)
	public void receiveMessageRequest(Packet packet) throws RetryException {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("message", "chatId", "userId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.MESSAGE_FEEDBACK, "Payload inválido"));
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
			throw new RetryException(messageServiceResponse.message());
		} else if (messageServiceResponse.status() == Status.VALIDATION_ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE_FEEDBACK, messageServiceResponse.message()));
			return;
		}

		ServiceResponse userServiceResponse = this.userService.loadUsersIdsFromChat(payload);
		if (userServiceResponse.status() == Status.VALIDATION_ERROR) {
			this.externalHandler.sendPacketById(clientId, this.packetFactory.createErrorResponse(PayloadType.MESSAGE_FEEDBACK, userServiceResponse.message()));
			return;
		}
		MessageCreateDto messageCreateDto = (MessageCreateDto) messageServiceResponse.payload();
		payload.put("senderUsername", messageCreateDto.getSenderUsername());
		payload.put("sentAt", messageCreateDto.getSentAt());

		this.externalHandler.sendPacketById(clientId, this.packetFactory.createOkResponse(PayloadType.MESSAGE_FEEDBACK, "Mensagem enviada com sucesso"));
		this.broadCastClientMessage((List<Long>) userServiceResponse.payload(), senderId, payload);
	}

	private void receiveMessageListing(Packet packet) {
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

	private void receiveGetUserChatId(Packet packet) {
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
		var responsePayload = new JSONObject(Map.of("chatId", serviceResponse.payload(), "targetUsername", payload.getString("targetUsername")));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GET_USER_CHAT_ID, responsePayload,
				serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveChatUsersListing(Packet packet) {
		JSONObject payload = packet.getPayload();

		var missingFields = JSONValidator.validate(payload, List.of("userId", "chatId"));
		if (!missingFields.isEmpty()) {
			this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(PayloadType.CHAT_USERS_LISTING, "Payload inválido"));
			logger.error("Invalid payload");
			return;
		}

		if (!this.authenticateClient(packet, PayloadType.CHAT_USERS_LISTING)) {
			return;
		}

		ServiceResponse serviceResponse = this.userService.loadUserNamesFromChatId(packet.getPayload());
		var usernames = serviceResponse.payload();
		var responsePayload = isNull(usernames) ? null : new JSONObject(Map.of("usernames", usernames));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.CHAT_USERS_LISTING, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	public void whenMaxAttempts(Packet packet, String message) {
		this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createErrorResponse(packet.getPayloadType(), message));
	}

	private void broadCastClientMessage(List<Long> targetUsersIds, Long senderId, JSONObject payload) {
		for (Long targetUserId : targetUsersIds) {
			if (targetUserId.equals(senderId)) {
				continue;
			}

			String targetUserClientId = this.userContextMap.getClientId(targetUserId);

			if (targetUserClientId != null) {
				this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createOkResponse(PayloadType.MESSAGE, payload));
			} else {
				payload.put("targetUserId", targetUserId);
				this.internalHandler.sendPacketById(this.gatewayId.getString(), this.packetFactory.createRequest(PayloadType.MESSAGE_FORWARDING, payload));
			}
		}
	}

}
