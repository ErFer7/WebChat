package com.ufsc.webchat.server;

import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.utils.SharedString;
import com.ufsc.webchat.utils.UserContextMap;

public class ManagerThread implements Manager {

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
		this.id = randomUUID().toString();
		this.externalHandler = serverHandler;
		this.internalHandler = clientHandler;
		this.gatewayHost = this.internalHandler.getGatewayHost();
		this.gatewayPort = this.internalHandler.getGatewayPort();
		this.packetFactory = new PacketFactory(this.id, HostType.APPLICATION);

		UserContextMap userContextMap = new UserContextMap();
		SharedString gatewayId = new SharedString();

		this.gatewayPacketProcessor = new GatewayPacketProcessor(this.externalHandler, this.internalHandler, this.packetFactory, userContextMap, gatewayId);
		this.clientPacketProcessor = new ClientPacketProcessor(this.externalHandler, this.internalHandler, this.packetFactory, userContextMap, gatewayId);
	}

	@Override
	public void run() {
		logger.info("Running manager");

		this.connect(this.gatewayHost, this.gatewayPort);

		try {
			if (this.internalHandler.getReadyLock().tryAcquire(5, TimeUnit.SECONDS)) {
				logger.info("Manager ready");
			} else {
				logger.error("Could not acquire ready lock");
			}
		} catch (InterruptedException exception) {
			logger.error("Lock error: {}", exception.getMessage());
		} finally {
			this.internalHandler.getReadyLock().release();
		}

		while (true) {
			if (!this.internalHandler.getInternalChannel().isConnected() && !this.retryConnection(this.gatewayHost, this.gatewayPort)) {
				return;
			}
		}
	}

	private void connect(String host, int port) {
		logger.info("Connecting");
		try {
			this.internalHandler.getInternalChannel().connect(new InetSocketAddress(InetAddress.getByName(host), port));
		} catch (IOException exception) {
			logger.error("Exception: {}", exception.getMessage());
		}
	}

	public boolean retryConnection(String host, int port) {
		int tries = 0;
		int maxRetries = 10;
		int waitTime = 5000;

		while (!this.internalHandler.getInternalChannel().isConnected()) {
			logger.info("Trying to connect");

			try {
				if (this.internalHandler.getInternalChannel().isConnectionPending()) {
					if (this.internalHandler.getInternalChannel().finishConnect()) {
						logger.info("Connected");
						return true;
					} else {
						logger.error("Connection pending or failed");
					}
				} else {
					if (this.internalHandler.getInternalChannel().connect(new InetSocketAddress(InetAddress.getByName(host), port))) {
						logger.info("Connected");
						return true;
					} else {
						logger.error("Connection pending or failed");
					}
				}

			} catch (IOException ioException) {
				logger.error("Exception: {}", ioException.getMessage());
			}

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

	public void sendGatewayClientDisconnectionRequest(String clientId) {
		this.clientPacketProcessor.receiveClientDisconnection(clientId);
	}
}
