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
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;

public class WebChatManagerThread extends Thread {

	private final WebChatServerHandler serverHandler;  // TODO: usar
	private final WebChatClientHandler clientHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private boolean registered;
	private final Semaphore registerSemaphore;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private static final Logger logger = LoggerFactory.getLogger(WebChatManagerThread.class);
	private final HashMap<Long, String> userIdTokenMap;

	public WebChatManagerThread(WebChatServerHandler serverHandler, WebChatClientHandler clientHandler) {
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
			if (this.clientHandler.getClientChannel().isConnected()) {
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

	private void sendGatewayConnectionRequest() {
		logger.info("Sending connection request to gateway");

		Packet packet = this.packetFactory.createGatewayConnectionRequest(this.gatewayIdentifier, this.gatewayPassword);

		this.clientHandler.sendPacket(packet);

		try {
			this.registerSemaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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

	public void receiveClientRoutingRequest(Packet packet) {
		String host = packet.getHost();
		JSONObject payload = packet.getPayload();

		Long userId = payload.getLong("userId");
		String token = payload.getString("token");
		Status status = Status.OK;

		this.userIdTokenMap.put(userId, token);

		Packet newPacket = this.packetFactory.createClientRoutingResponse(status, userId, token);

		this.clientHandler.sendPacket(newPacket);
	}

	public void processGatewayPackets(Packet packet) {
		if (packet.getPayloadType() == PayloadType.CONNECTION) {
			this.receiveGatewayConnectionResponse(packet);
		} else if (packet.getPayloadType() == PayloadType.ROUTING) {
			this.receiveClientRoutingRequest(packet);
		}
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int waitTime = 1000;

		while (!this.clientHandler.getClientChannel().isConnected()) {
			logger.info("Trying to connect to gateway");

			try {
				this.clientHandler.getClientChannel().connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
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
