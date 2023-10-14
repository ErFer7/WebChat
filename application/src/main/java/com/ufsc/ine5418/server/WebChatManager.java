package com.ufsc.ine5418.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.protocol.PacketFactory;
import com.ufsc.ine5418.protocol.enums.HostType;
import com.ufsc.ine5418.protocol.enums.Status;
import com.ufsc.ine5418.utils.Logger;

public class WebChatManager extends Manager {

	private final WebChatServerHandler serverHandler;
	private final WebChatClientHandler clientHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private final PacketFactory packetFactory;
	private boolean registered;
	private final Semaphore registerSemaphore;
	private final String gatewayIdentifier;
	private final String gatewayPassword;

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

	public void setHost(String host) {
		this.packetFactory.setHost(host);
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
		Logger.log(this.getClass().getSimpleName(), "Received connection response from gateway");

		if (packet.getStatus() == Status.OK) {
			this.registered = true;
		}

		this.registerSemaphore.release();
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int timeout = 1000;

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
					sleep(timeout);
				} catch (InterruptedException interruptedException) {
					Logger.log(this.getClass().getSimpleName(), "Exception: " + interruptedException.getMessage());
				}
			}
		}

		return true;
	}
}
