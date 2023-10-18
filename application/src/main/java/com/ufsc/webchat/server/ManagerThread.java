package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
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
		JSONObject payload = packet.getPayload();

		String userAddr = packet.getHost();
		Long userId = payload.getLong("userId");

		if (this.authenticateClient(userId, packet.getToken())) {
			userIdHostMap.put(userId, packet.getHost());

			this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.OK));
		} else {
			this.serverHandler.sendPacket(userAddr, this.packetFactory.createClientConnectionResponse(Status.ERROR));
		}
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
		// TODO: Implementar o fluxo de envio de mensagens
	}

	private void receiveMessageListing(Packet packet) {
		// TODO: Implementar o fluxo de listagem de mensagens
	}

	private void receiveDisconnectionRequest(Packet packet) {
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
		case DISCONNECTION -> this.receiveDisconnectionRequest(packet);
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
