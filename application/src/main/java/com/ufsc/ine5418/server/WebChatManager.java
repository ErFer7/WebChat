package com.ufsc.ine5418.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.protocol.enums.HostType;
import com.ufsc.ine5418.protocol.enums.OperationType;
import com.ufsc.ine5418.protocol.enums.PayloadType;
import com.ufsc.ine5418.utils.Logger;

public class WebChatManager extends ServerManager {

	private final WebChatServerHandler serverHandler;
	private final WebChatClientHandler clientHandler;
	private final String gatewayHost;
	private final int gatewayPort;
	private boolean registered;

	public WebChatManager(WebChatServerHandler serverHandler, WebChatClientHandler clientHandler) {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.gatewayHost = this.clientHandler.getGatewayHost();
		this.gatewayPort = this.clientHandler.getGatewayPort();
		this.registered = false;
	}

	@Override
	public void run() {
		Logger.log(this.getClass().getSimpleName(), "Thread started");

		boolean connected = this.connectToGateway();

		if (!connected) {
			return;
		}

		while (true) {
			if (this.clientChannel.isConnected()) {
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

		Packet packet = new Packet(this.clientHandler.getSession().getLocalAddress().toString(), HostType.APPLICATION, null, null, OperationType.REQUEST, PayloadType.CONNECTION, null);

		this.clientHandler.sendPacket(packet);
		this.registered = true;
	}

	private boolean connectToGateway() {
		int tries = 0;
		int maxRetries = 5;
		int timeout = 1000;

		while (!this.clientChannel.isConnected()) {
			Logger.log(this.getClass().getSimpleName(), "Trying to connect to gateway");

			try {
				this.clientChannel.connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
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

		Logger.log(this.getClass().getSimpleName(), "Connected to gateway");
		return true;
	}
}
