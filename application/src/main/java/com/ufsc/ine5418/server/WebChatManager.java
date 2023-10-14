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

	public WebChatManager(WebChatServerHandler serverHandler, WebChatClientHandler clientHandler) {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.gatewayHost = this.clientHandler.getGatewayHost();
		this.gatewayPort = this.clientHandler.getGatewayPort();
	}

	@Override
	public void run() {
		Logger.log(this.getClass().getSimpleName(), "Thread started");

		try {
			this.clientChannel.connect(new InetSocketAddress(InetAddress.getByName(this.gatewayHost), this.gatewayPort));
		} catch (IOException e) {
			Logger.log(this.getClass().getSimpleName(), "Exception: " + e.getMessage());
		}

		while (true) {
			try {
				sleep(1000);

				if (this.clientChannel.isConnected()) {
					Logger.log(this.getClass().getSimpleName(), "Sending connection request to gateway");

					Packet packet = new Packet(this.clientHandler.getSession().getLocalAddress().toString(), HostType.APPLICATION, null, null, OperationType.REQUEST, PayloadType.CONNECTION, null);

					this.clientHandler.sendPacket(packet);
				}
			} catch (Exception ex) {
				Logger.log(this.getClass().getSimpleName(), "Exception: " + ex.getMessage());
			}
		}
	}
}
