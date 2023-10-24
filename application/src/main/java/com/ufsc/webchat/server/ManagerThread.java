package com.ufsc.webchat.server;

import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.utils.SharedString;
import com.ufsc.webchat.utils.UserContextMap;

public class ManagerThread extends Thread {

	private final String id;
	private final ExternalHandler externalHandler;
	private final InternalHandler internalHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private final GatewayPacketProcessor gatewayPacketProcessor;
	private final ClientPacketProcessor clientPacketProcessor;
	private static final Logger logger = LoggerFactory.getLogger(ManagerThread.class);

	public ManagerThread(ExternalHandler serverHandler, InternalHandler clientHandler) {
		super("manager-thread");

		this.id = randomUUID().toString();
		this.externalHandler = serverHandler;
		this.internalHandler = clientHandler;
		this.gatewayHost = this.internalHandler.getGatewayHost();
		this.gatewayPort = this.internalHandler.getGatewayPort();
		this.packetFactory = new PacketFactory(this.id, HostType.APPLICATION);

		UserContextMap userContextMap = new UserContextMap();
		HashMap<Long, String> externalUserIdApplicationIdMap = new HashMap<>();
		HashMap<Long, JSONObject> tempMessageMap = new HashMap<>();
		SharedString gatewayId = new SharedString();

		this.gatewayPacketProcessor = new GatewayPacketProcessor(this, this.externalHandler, this.internalHandler, this.packetFactory,
				userContextMap, externalUserIdApplicationIdMap, tempMessageMap, gatewayId);
		this.clientPacketProcessor = new ClientPacketProcessor(this.externalHandler, this.internalHandler, this.packetFactory,
				userContextMap, externalUserIdApplicationIdMap, tempMessageMap, gatewayId);
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

	public boolean connect(String host, int port) {
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

	public void processClientPackets(Packet packet) {
		this.clientPacketProcessor.process(packet);
	}

	public void processGatewayPackets(Packet packet) {
		this.gatewayPacketProcessor.process(packet);
	}

	public void sendClientHandshakeInfo(IWebSocketSession session) {
		this.externalHandler.sendPacketBySession(session, this.packetFactory.createHandshakeInfo(session.getRemoteAddress().toString()));
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
