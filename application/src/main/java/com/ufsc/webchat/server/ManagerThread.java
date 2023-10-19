package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;

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
	private final HashMap<Long, String> userIdTokenMap;
	private final HashMap<Long, String> userIdHostMap;
	private final HashMap<Long, String> userIdApplicationHostMap;

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
		this.userIdTokenMap = new HashMap<>();
		this.userIdHostMap = new HashMap<>();
		this.userIdApplicationHostMap = new HashMap<>();
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

	private void receiveUserApplicationResponse(Packet packet) {
		// TODO: Implementar
	}

	private void sendMessage() {
		// TODO: Implementar
	}

	private void sendMessageRedirection() {
		// TODO: Implementar
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

	public void receiveClientRoutingRequest(Packet packet) {
		JSONObject payload = packet.getPayload();

		Long userId = payload.getLong("userId");
		String token = payload.getString("token");
		Status status = Status.OK;

		this.userIdTokenMap.put(userId, token);

		this.sendPacketToGateway(this.packetFactory.createClientRoutingResponse(status, userId, token));
	}

	private void receiveClientConnectionRequest(Packet packet) {
		String userAddr = packet.getHost();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		if (!this.authenticateClient(userId, packet.getToken())) {
			this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.ERROR));
			return;
		}

		this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.OK));
	}

	private void receiveUserListingRequest(Packet packet) {
		// TODO: Implementar o fluxo de listagem de usuários
	}

	private void receiveChatCreationRequest(Packet packet) {
		// TODO: Implementar o fluxo de criação de conversas
	}

	private void receiveChatListingRequest(Packet packet) {
		// TODO: Implementar o fluxo de listagem de conversas
	}

	private void receiveChatAdditionRequest(Packet packet) {
		// TODO: Implementar o fluxo de adição de usuários em conversas
	}

	private void receiveMessage(Packet packet) {
		String userAddr = packet.getHost();
		JSONObject payload = packet.getPayload();
		Long userId = payload.getLong("userId");

		if (!this.authenticateClient(userId, packet.getToken())) {
			this.serverHandler.sendPacket(userAddr, this.packetFactory.createAuthenticationErrorResponse(PayloadType.MESSAGE));
			return;
		}

		Long chatId = payload.getLong("chatId");

		List<Long> targetUsersIds = new ArrayList<>();  // TODO: Obter todos os usuários da conversa
		Iterator<Long> userIdsIterator = targetUsersIds.iterator();

		// Envia as mensagens para os usuários da conversa que estão conectados neste servidor
		while (userIdsIterator.hasNext()) {
			Long targetUserId = userIdsIterator.next();
			String host = this.userIdHostMap.get(targetUserId);

			if (host != null) {
				// TODO: Enviar

				try {
					userIdsIterator.remove();
				} catch (ConcurrentModificationException exception) {
					logger.error("Exception: {}", exception.getMessage());
				}
			}
		}

		userIdsIterator = targetUsersIds.iterator();

		// Envia as mensagens para os servidores que estão conectados com os usuários da conversa
		while (userIdsIterator.hasNext()) {
			Long targetUserId = userIdsIterator.next();
			String applicationHost = this.userIdApplicationHostMap.get(targetUserId);

			if (applicationHost != null) {
				// TODO: Enviar

				try {
					userIdsIterator.remove();
				} catch (ConcurrentModificationException exception) {
					logger.error("Exception: {}", exception.getMessage());
				}
			}
		}

		// TODO: Enviar request para o gateway para encontrar o servidor em que o usuário está conectado
		// TODO: Enviar a mensagem para o servidor encontrado após o gateway responder

		this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.OK));
	}

	private void receiveMessageListing(Packet packet) {
		// TODO: Implementar o fluxo de listagem de mensagens
	}

	private void receiveClientDisconnectionRequest(Packet packet) {
		// TODO: Implementar o fluxo de desconexão
	}

	private void receiveApplicationConnectionResponse(Packet packet) {
		// TODO: Implementar o fluxo de resposta de conexão de aplicações
	}

	private void receiveMessageRedirection(Packet packet) {
		// TODO: Implementar o fluxo de redirecionamento de mensagens
	}

	public void processGatewayPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveGatewayConnectionResponse(packet);
		case ROUTING -> this.receiveClientRoutingRequest(packet);
		case USER_APPLICATION_SERVER -> this.receiveUserApplicationResponse(packet);
		}
	}

	public void processApplicationPackets(Packet packet) {
		switch (packet.getPayloadType()) {
			case CONNECTION -> this.receiveApplicationConnectionResponse(packet);
			case MESSAGE -> this.receiveMessageRedirection(packet);
		}
	}

	public void processClientPackets(Packet packet) {
		switch (packet.getPayloadType()) {
		case CONNECTION -> this.receiveClientConnectionRequest(packet);
		case USER_LISTING -> this.receiveUserListingRequest(packet);
		case CHAT_CREATION -> this.receiveChatCreationRequest(packet);
		case CHAT_LISTING -> this.receiveChatListingRequest(packet);
		case CHAT_ADDITION -> this.receiveChatAdditionRequest(packet);
		case MESSAGE -> this.receiveMessage(packet);
		case MESSAGE_LISTING -> this.receiveMessageListing(packet);
		case DISCONNECTION -> this.receiveClientDisconnectionRequest(packet);
		}
	}

	private boolean authenticateClient(Long userId, String token) {
		return this.userIdTokenMap.get(userId).equals(token);
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
}
