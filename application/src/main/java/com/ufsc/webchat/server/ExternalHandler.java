package com.ufsc.webchat.server;

import java.nio.channels.ServerSocketChannel;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.enums.HostType;

public class ExternalHandler extends Handler {

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.CLIENT) {
			((ManagerThread) this.managerThread).processClientPackets(packet);
		}
	}

	@Override protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		((ManagerThread) this.managerThread).sendClientHandshakeInfo(session);
	}

	@Override
	public ServerSocketChannel getInternalChannel() {
		return (ServerSocketChannel) this.internalChannel;
	}
}
