package com.ufsc.webchat.server;

import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.database.model.ChatDto;
import com.ufsc.webchat.database.service.ChatService;
import com.ufsc.webchat.database.service.MessageService;
import com.ufsc.webchat.database.service.UserService;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.UserContextMap;

public class ManagerThread extends Thread {

	private final String id;
	private final ExternalHandler externalHandler;
	private final InternalHandler internalHandler;
	private final String gatewayHost;
	private String gatewayId;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final UserContextMap userContextMap;
	private final HashMap<Long, String> externalUserIdApplicationIdMap;
	private final ChatService chatService;
	private final UserService userService;
	private final MessageService messageService;
	private final HashMap<Long, JSONObject> tempMessageMap;
	private static final Logger logger = LoggerFactory.getLogger(ManagerThread.class);

	public ManagerThread(ExternalHandler serverHandler, InternalHandler clientHandler) {
		super("manager-thread");

		this.id = randomUUID().toString();
		this.externalHandler = serverHandler;
		this.internalHandler = clientHandler;
		this.gatewayHost = this.internalHandler.getGatewayHost();
		this.gatewayPort = this.internalHandler.getGatewayPort();
		this.packetFactory = new PacketFactory(this.id, HostType.APPLICATION);
		this.gatewayIdentifier = System.getProperty("gatewayIdentifier");
		this.gatewayPassword = System.getProperty("gatewayPassword");
		this.userContextMap = new UserContextMap();
		this.chatService = new ChatService();
		this.userService = new UserService();
		this.messageService = new MessageService();
		this.externalUserIdApplicationIdMap = new HashMap<>();
		this.tempMessageMap = new HashMap<>();
	}

	@Override
	public void run() {
		logger.info("Thread started");

		boolean connected = this.connect(this.gatewayHost, this.gatewayPort);

		try {
			this.internalHandler.getReadyLock().tryAcquire(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			this.internalHandler.getReadyLock().release();
		}

		if (!connected) {
			return;
		}

		while (true) {
			if (!this.internalHandler.getInternalChannel().isConnected()) {
				connected = this.connect(this.gatewayHost, this.gatewayPort);

				if (!connected) {
					return;
				}
			}
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

	private boolean connect(String host, int port) {
		int tries = 0;
		int maxRetries = 5;
		int waitTime = 1000;

		while (!this.internalHandler.getInternalChannel().isConnected()) {
			logger.info("Trying to connect");

			try {
				this.internalHandler.getInternalChannel().connect(new InetSocketAddress(InetAddress.getByName(host), port));
				logger.info("Connected");
				return true;
			} catch (IOException ioException) {
				logger.error("Exception: {}", ioException.getMessage());

				tries++;

				if (tries >= maxRetries) {
					logger.error("Could not connect");
					return false;
				}

				try {
					sleep(waitTime);
				} catch (InterruptedException interruptedException) {
					logger.error("Exception: {}", interruptedException.getMessage());
				}
			}
		}

		return true;
	}

	public void processGatewayPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case HOST -> this.receiveGatewayHostInfo(packet);
		case CONNECTION -> this.receiveGatewayConnectionResponse(packet);
		case ROUTING -> this.receiveGatewayClientRoutingRequest(packet);
		case USER_APPLICATION_SERVER -> this.receiveGatewayUserApplicationResponse(packet);
		case DISCONNECTION -> this.receiveGatewayClientDisconnectionResponse(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	public void processApplicationPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveApplicationConnectionResponse(packet);
		case MESSAGE -> this.receiveApplicationMessageRedirection(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	public void processClientPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveClientConnectionRequest(packet);
		case USER_LISTING -> this.receiveClientUserListingRequest(packet);
		case GROUP_CHAT_CREATION -> this.receiveClientGroupChatCreationRequest(packet);
		case CHAT_LISTING -> this.receiveClientChatListingRequest(packet);
		case GROUP_CHAT_ADDITION -> this.receiveClientGroupChatAdditionRequest(packet);
		case MESSAGE -> this.receiveClientMessage(packet);
		case MESSAGE_LISTING -> this.receiveClientMessageListing(packet);
		case DISCONNECTION -> this.receiveClientDisconnectionRequest(packet);
		case GET_USER_CHAT_ID -> this.receiveClientGetUserChatId(packet);
		default -> logger.warn("Unexpected packet type: {}", packet.getPayloadType());
		}
	}

	public void sendClientHandshakeInfo(IWebSocketSession session) {
		this.externalHandler.sendPacketBySession(session, this.packetFactory.createHandshakeInfo(session.getRemoteAddress().toString()));
	}

	public void receiveGatewayHostInfo(Packet packet) {
		String host = packet.getPayload().getString("host");
		this.gatewayId = packet.getId();
		this.internalHandler.associateIdToHost('/' + this.gatewayHost + ':' + this.gatewayPort, this.gatewayId);

		logger.info("Sending connection request to gateway");

		int externalPort = this.externalHandler.getInternalChannel().socket().getLocalPort();
		Packet response = this.packetFactory.createGatewayConnectionRequest(this.gatewayIdentifier, this.gatewayPassword, host, externalPort);
		this.internalHandler.sendPacketById(this.gatewayId, response);
	}

	public void receiveGatewayConnectionResponse(Packet packet) {
		if (packet.getStatus() == Status.OK) {
			logger.info("Gateway authentication successful");
			JSONObject payload = packet.getPayload();

			this.packetFactory.setToken(payload.getString("token"));
		} else {
			logger.warn("Gateway authentication failed");
		}
	}

	public void receiveGatewayClientRoutingRequest(Packet packet) {
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		String token = payload.getString("token");

		this.userContextMap.add(userId, token);

		this.internalHandler.sendPacketById(this.gatewayId, this.packetFactory.createApplicationClientRoutingResponse(Status.OK, userId, token));
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

		this.internalHandler.sendPacketById(this.gatewayId, this.packetFactory.createApplicationClientDisconnectingRequest(userId));
	}

	private void receiveGatewayClientDisconnectionResponse(Packet packet) {
		Long userId = packet.getPayload().getLong("userId");

		this.externalHandler.sendPacketById(this.userContextMap.getClientId(userId), this.packetFactory.createApplicationClientDisconnectionResponse());
		this.userContextMap.remove(userId);
	}

	private void receiveGatewayUserApplicationResponse(Packet packet) {
		// TODO: Implementar [CONTINUAR DAQUI]
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
		JSONObject responsePayload = new JSONObject(Map.of("chatId", serviceResponse.payload()));
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
		ServiceResponse serviceResponse = this.chatService.loadChatIdByUsers(packet.getPayload());
		var responsePayload = new JSONObject(Map.of("chatId", serviceResponse.payload()));
		var responsePacket = this.packetFactory.createGenericClientResponse(serviceResponse.status(), PayloadType.GET_USER_CHAT_ID, responsePayload, serviceResponse.message());
		this.externalHandler.sendPacketById(packet.getId(), responsePacket);
	}

	private void receiveApplicationConnectionResponse(Packet packet) {
		// TODO: Implementar o fluxo de resposta de conexão de aplicações
	}

	private void receiveApplicationMessageRedirection(Packet packet) {
		Long targetUserId = packet.getPayload().getLong("targetId");

		String targetUserClientId = this.userContextMap.getClientId(targetUserId);

		this.externalHandler.sendPacketById(targetUserClientId, this.packetFactory.createApplicationMessageResponse(Status.OK, packet.getPayload()));
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
					this.internalHandler.sendPacketById(this.gatewayId, this.packetFactory.createApplicationUserApplicationServerRequest(messageId.toString(), targetUserId));
				}
			}
		}
	}

	private void sendApplicationConnectionRequest() {
		// TODO: Implementar
	}

	private void sendUserApplicationRequest() {
		// TODO: Implementar
	}

	private void sendMessage() {
		// TODO: Implementar
	}

	private void sendMessageRedirection() {
		// TODO: Implementar
	}
}
