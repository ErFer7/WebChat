package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.utils.Logger;

public class WebChatManager extends Manager {

	private final WebChatServerHandler serverHandler;  // TODO: usar
	private final WebChatClientHandler clientHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private boolean registered;
	private final Semaphore registerSemaphore;
	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final HashMap<String, String> userIdTokenMap;

	public WebChatManager(WebChatServerHandler serverHandler, WebChatClientHandler clientHandler, String gatewayIdentifier, String gatewayPassword) {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.gatewayHost = this.clientHandler.getGatewayHost();
		this.gatewayPort = this.clientHandler.getGatewayPort();
		this.packetFactory = new PacketFactory(HostType.APPLICATION);
		this.registered = false;
		this.registerSemaphore = new Semaphore(0);
		this.gatewayIdentifier = gatewayIdentifier;
		this.gatewayPassword = gatewayPassword;
		this.userIdTokenMap = new HashMap<>();
	}

	@Override
	public void run() {
		Logger.log(this.getClass().getSimpleName(), "Thread started");

		boolean connected = this.connectToGateway();

		if (!connected) {
			return;
		}

		try {
			this.clientHandler.getReadySemaphore().acquire();
			this.packetFactory.setHost(this.clientHandler.getSession().getLocalAddress().toString());
			this.clientHandler.getReadySemaphore().release();
		} catch (Exception exception) {
			Logger.log(this.getClass().getSimpleName(), "Exception: " + exception.getMessage());
			return;
		}

		while (true) {
			if (this.clientHandler.getClientChannel().isConnected()) {
				// TODO: Implementar o fluxo de retry e controle do token
				if (!this.registered) {
					this.sendConnectionRequest();
				}
			} else {
				connected = this.connectToGateway();

				if (!connected) {
					return;
				}
			}
		}
	}

	private void sendConnectionRequest() {
		Logger.log(this.getClass().getSimpleName(), "Sending connection request to gateway");

		Packet packet = this.packetFactory.createGatewayConnectionRequest(this.gatewayIdentifier, this.gatewayPassword);

		this.clientHandler.sendPacket(packet);

		try {
			this.registerSemaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void receiveConnectionResponse(Packet packet) {

		if (packet.getStatus() == Status.OK) {
			Logger.log(this.getClass().getSimpleName(), "Gateway authentication successful");
			this.registered = true;
			JSONObject payload = packet.getPayload();

			this.packetFactory.setToken(payload.getString("token"));
		} else {
			Logger.log(this.getClass().getSimpleName(), "Gateway authentication failed");
		}

		this.registerSemaphore.release();
	}

	public void receiveRoutingRequest(Packet packet) {
		String host = packet.getHost();
		JSONObject payload = packet.getPayload();

		String userId = payload.getString("userId");
		String token = payload.getString("token");
		Status status = Status.OK;

		this.userIdTokenMap.put(userId, token);

		Packet newPacket = this.packetFactory.createRoutingResponse(status, userId, token);

		this.clientHandler.sendPacket(newPacket);
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int waitTime = 1000;

		while (!this.clientHandler.getClientChannel().isConnected()) {
			Logger.log(this.getClass().getSimpleName(), "Trying to connect to gateway");

			try {
				this.clientHandler.getClientChannel().connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
				Logger.log(this.getClass().getSimpleName(), "Connected to gateway");
				return true;
			} catch (IOException ioException) {
				Logger.log(this.getClass().getSimpleName(), "Exception: " + ioException.getMessage());

				tries++;

				if (tries >= maxRetries) {
					Logger.log(this.getClass().getSimpleName(), "Could not connect to gateway");
					return false;
				}

				try {
					sleep(waitTime);
				} catch (InterruptedException interruptedException) {
					Logger.log(this.getClass().getSimpleName(), "Exception: " + interruptedException.getMessage());
				}
			}
		}

		return true;
	}
}
