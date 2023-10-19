package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.ufsc.webchat.database.service.ChatService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.UserContextMap;

public class ManagerThread extends Thread {

	private final ExternalHandler serverHandler;  // TODO: usar
	private final InternalHandler clientHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private boolean registered;
	private final Semaphore registerSemaphore;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private static final Logger logger = LoggerFactory.getLogger(ManagerThread.class);
	private final UserContextMap userContextMap;
	private final HashMap<Long, String> externalUserIdApplicationHost = new HashMap<>();
	private final ChatService chatService = new ChatService();

	public ManagerThread(ExternalHandler serverHandler, InternalHandler clientHandler) {
		super("manager-thread");

		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.gatewayHost = this.clientHandler.getGatewayHost();
		this.gatewayPort = this.clientHandler.getGatewayPort();
		this.packetFactory = new PacketFactory(HostType.APPLICATION);
		this.registered = false;
		this.registerSemaphore = new Semaphore(0);
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

		try {
			this.clientHandler.getReadySemaphore().acquire();
			this.packetFactory.setHost(this.clientHandler.getSession().getLocalAddress().toString());
			this.clientHandler.getReadySemaphore().release();
		} catch (Exception exception) {
			logger.error("Exception: {}", exception.getMessage());
			return;
		}

		while (true) {
			if (this.clientHandler.getInternalChannel().isConnected()) {
				// TODO: Implementar o fluxo de retry e controle do token
				if (!this.registered) {
					this.sendGatewayConnectionRequest();
				}
			} else {
				connected = this.connectToGateway();

				if (!connected) {
					return;
				}
			}
		}
	}

	private void sendPacketToGateway(Packet packet) {
		this.clientHandler.sendPacket('/' + this.gatewayHost + ':' + this.gatewayPort, packet);
	}

	private boolean authenticateClient(Packet packet, PayloadType payloadType) {
		String userAddr = packet.getHost();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		if (!this.userContextMap.getUserToken(userId).equals(packet.getToken())) {
			this.serverHandler.sendPacket(userAddr, this.packetFactory.createAuthenticationErrorResponse(payloadType));
			return false;
		}

		return true;
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int waitTime = 1000;

		while (!this.clientHandler.getInternalChannel().isConnected()) {
			logger.info("Trying to connect to gateway");

			try {
				this.clientHandler.getInternalChannel().connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
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

	public void receiveGatewayConnectionResponse(Packet packet) {
		if (packet.getStatus() == Status.OK) {
			logger.info("Gateway authentication successful");
			this.registered = true;
			JSONObject payload = packet.getPayload();

			this.packetFactory.setToken(payload.getString("token"));
		} else {
			logger.warn("Gateway authentication failed");
		}

		this.registerSemaphore.release();
	}

	public void receiveGatewayClientRoutingRequest(Packet packet) {
		JSONObject payload = packet.getPayload();

		Long userId = payload.getLong("userId");
		String token = payload.getString("token");
		Status status = Status.OK;

		this.userContextMap.add(userId, token);

		this.sendPacketToGateway(this.packetFactory.createClientRoutingResponse(status, userId, token));
	}

	private void receiveClientConnectionRequest(Packet packet) {
		if(!this.authenticateClient(packet, PayloadType.CONNECTION)) {
			return;
		}

		String userAddr = packet.getHost();
		this.userContextMap.setUserHost(packet.getPayload().getLong("userId"), userAddr);

		this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.OK));
	}

	private void receiveClientDisconnectionRequest(Packet packet) {
		if(!this.authenticateClient(packet, PayloadType.DISCONNECTION)) {
			return;
		}

		String userId = packet.getPayload().getString("userId");

		this.sendPacketToGateway(this.packetFactory.createApplicationClientDisconnectingRequest(userId));
	}

	private void receiveGatewayClientDisconnectionResponse(Packet packet) {
		Long userId = packet.getPayload().getLong("userId");

		this.serverHandler.sendPacket(this.userContextMap.getUserHost(userId), this.packetFactory.createApplicationClientDisconnectionResponse());
		this.userContextMap.remove(userId);
	}

	private void receiveGatewayUserApplicationResponse(Packet packet) {
		// TODO: Implementar
	}

	private void receiveUserListingRequest(Packet packet) {
		// TODO: Implementar o fluxo de listagem de usuários
	}

	private void receiveClientGroupChatCreationRequest(Packet packet) { // doing now
		if(!this.authenticateClient(packet, PayloadType.GROUP_CHAT_CREATION)) {
			return;
		}

		JSONObject payload = packet.getPayload();

		String userName = payload.getString("userName");
		List<String> membersName= payload.getJSONArray("membersName").toList().stream().map(Object::toString).toList();

		for (String member : membersName) {

		}
		// TODO: Implementar o fluxo de criação de conversas

		this.serverHandler.sendPacket(packet.getHost(), this.packetFactory.createGroupChatCreationResponse(Status.OK));
	}

	private void receiveClientChatListingRequest(Packet packet) {
		if(!this.authenticateClient(packet, PayloadType.CHAT_LISTING)) {
			return;
		}

		JSONObject payload = packet.getPayload();

		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		// TODO: Implementar o fluxo de listagem de conversas
	}

	private void receiveClientGroupChatAdditionRequest(Packet packet) {
		if(!this.authenticateClient(packet, PayloadType.GROUP_CHAT_ADDITION)) {
			return;
		}

		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");
		Long addedUserId = payload.getLong("addedUserId");

		// TODO: Implementar o fluxo de adição de usuários em conversas
	}

	private void receiveClientMessage(Packet packet) {
		if(!this.authenticateClient(packet, PayloadType.MESSAGE)) {
			return;
		}

		String userAddr = packet.getHost();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		Long chatId = payload.getLong("chatId");

		List<Long> targetUsersIds = new ArrayList<>();  // TODO: Obter todos os usuários da conversa

		for (Long targetUserId : targetUsersIds) {
			String targetUserHost = this.userContextMap.getUserHost(targetUserId);

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

	private void sendGatewayConnectionRequest() {
		logger.info("Sending connection request to gateway");

		Packet packet = this.packetFactory.createGatewayConnectionRequest(this.gatewayIdentifier, this.gatewayPassword);

		this.sendPacketToGateway(packet);

		try {
			this.registerSemaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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
