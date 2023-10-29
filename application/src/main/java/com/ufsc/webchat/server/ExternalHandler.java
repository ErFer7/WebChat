package com.ufsc.webchat.server;

import java.nio.channels.ServerSocketChannel;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.enums.HostType;

public class ExternalHandler extends Handler {

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.CLIENT) {
			((ManagerImpl) this.managerThread).processClientPackets(packet);
		}
	}

	@Override protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);

		((ManagerImpl) this.managerThread).sendClientHandshakeInfo(session);
	}

	@Override protected void sessionClosed(IWebSocketSession session) {
		if (this.sessions.getProcessIdBySessionId(session.getId()) != null) {
			((ManagerImpl) this.managerThread).sendGatewayClientDisconnectionRequest(this.sessions.getProcessIdBySessionId(session.getId()));
		}

		super.sessionClosed(session);
	}

	@Override
	public ServerSocketChannel getInternalChannel() {
		return (ServerSocketChannel) this.internalChannel;
	}
}
