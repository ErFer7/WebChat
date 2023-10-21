package com.ufsc.webchat.server;

import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.database.service.ChatService;
import com.ufsc.webchat.model.ServiceAnswer;
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
	private static final Logger logger = LoggerFactory.getLogger(ManagerThread.class);
	private final UserContextMap userContextMap;
	private final HashMap<Long, String> externalUserIdApplicationHost = new HashMap<>();
	private final ChatService chatService = new ChatService();

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
	}

	@Override
	public void run() {
		logger.info("Thread started");

		boolean connected = this.connectToGateway();

		if (!connected) {
			return;
		}

		while (true) {
			if (!this.internalHandler.getInternalChannel().isConnected()) {
//				connected = this.connectToGateway();

				if (!connected) {
					return;
				}
			}
		}
	}

	private boolean authenticateClient(Packet packet, PayloadType payloadType) {
		String userAddr = packet.getId();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		if (!this.userContextMap.getToken(userId).equals(packet.getToken())) {
			this.externalHandler.sendPacketById(userAddr, this.packetFactory.createAuthenticationErrorResponse(payloadType));
			return false;
		}

		return true;
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int waitTime = 1000;

		while (!this.internalHandler.getInternalChannel().isConnected()) {
			logger.info("Trying to connect to gateway");

			try {
				this.internalHandler.getInternalChannel().connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
				logger.info("Connected to gateway");
				return true;
			} catch (IOException ioException) {
				logger.error("Exception: {}", ioException.getMessage());

				tries++;

				if (tries >= maxRetries) {
					logger.error("Could not connect to gateway");
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
		}
	}

	public void processApplicationPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveApplicationConnectionResponse(packet);
		case MESSAGE -> this.receiveApplicationMessageRedirection(packet);
		}
	}

	public void processClientPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveClientConnectionRequest(packet);
		case USER_LISTING -> this.receiveUserListingRequest(packet);
		case GROUP_CHAT_CREATION -> this.receiveClientGroupChatCreationRequest(packet);
		case CHAT_LISTING -> this.receiveClientChatListingRequest(packet);
		case GROUP_CHAT_ADDITION -> this.receiveClientGroupChatAdditionRequest(packet);
		case MESSAGE -> this.receiveClientMessage(packet);
		case MESSAGE_LISTING -> this.receiveClientMessageListing(packet);
		case DISCONNECTION -> this.receiveClientDisconnectionRequest(packet);
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
		Status status = Status.OK;

		this.userContextMap.add(userId, token);

		this.internalHandler.sendPacketById(this.gatewayId, this.packetFactory.createApplicationClientRoutingResponse(status, userId, token));
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
		// TODO: Implementar
	}

	private void receiveUserListingRequest(Packet packet) {
		// TODO: Implementar o fluxo de listagem de usuários
	}

	private void receiveClientGroupChatCreationRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_CREATION)) {
			return;
		}
		ServiceAnswer serviceAnswer = this.chatService.saveChatGroup(packet.getPayload());
		this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createGroupChatCreationResponse(serviceAnswer.status(), serviceAnswer.message()));
	}

	private void receiveClientChatListingRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.CHAT_LISTING)) {
			return;
		}

		JSONObject payload = packet.getPayload();

		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		// TODO: Implementar o fluxo de listagem de conversas
	}

	private void receiveClientGroupChatAdditionRequest(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.GROUP_CHAT_ADDITION)) {
			return;
		}
		ServiceAnswer serviceAnswer = this.chatService.addToChatGroup(packet.getPayload());
		this.externalHandler.sendPacketById(packet.getId(), this.packetFactory.createGroupChatAdditionResponse(serviceAnswer.status(), serviceAnswer.message()));
	}

	private void receiveClientMessage(Packet packet) {
		if (!this.authenticateClient(packet, PayloadType.MESSAGE)) {
			return;
		}

		String userAddr = packet.getId();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		Long chatId = payload.getLong("chatId");

		List<Long> targetUsersIds = new ArrayList<>();  // TODO: Obter todos os usuários da conversa

		for (Long targetUserId : targetUsersIds) {
			String targetUserHost = this.userContextMap.getClientId(targetUserId);

			if (targetUserHost != null) {
				// TODO: Enviar para o cliente
			} else {
				String applicationHost = this.externalUserIdApplicationHost.get(targetUserId);

				if (applicationHost != null) {
					// TODO: Enviar para o servidor
				} else {
					// TODO: Enviar request para o gateway para encontrar o servidor em que o usuário está conectado
					// Talvez seja bom salvar a mensagem temporariamente aqui, que nem é feito com os hosts de clientes no gateway
				}
			}
		}
	}

	private void receiveClientMessageListing(Packet packet) {
		// TODO: Implementar o fluxo de listagem de mensagens
	}

	private void receiveApplicationConnectionResponse(Packet packet) {
		// TODO: Implementar o fluxo de resposta de conexão de aplicações
	}

	private void receiveApplicationMessageRedirection(Packet packet) {
		// TODO: Implementar o fluxo de redirecionamento de mensagens
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
