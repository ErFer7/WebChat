package com.ufsc.ine5418.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.protocol.PacketFactory;
import com.ufsc.ine5418.utils.Logger;

public abstract class ServerHandler extends Handler {

	protected final PacketFactory packetFactory;
	protected final ConcurrentMap<String, IStreamSession> sessions;
	private boolean packetFactoryHostSet;


	public ServerHandler(PacketFactory packetFactory) {
		this.sessions = new ConcurrentHashMap<String, IStreamSession>();
		this.packetFactory = packetFactory;
		this.packetFactoryHostSet = false;
	}

	public abstract void readPacket(Packet packet);

	public void sendPacket(String host, Packet packet) {
		IStreamSession session = sessions.get(host);
		session.writenf(new TextFrame(packet.toString()));
	}

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		switch (event) {
		case READY:
			if (!this.packetFactoryHostSet) {
				this.packetFactory.setHost(session.getLocalAddress().toString());
				this.packetFactoryHostSet = true;
			}

			session.getAttributes().put("user-id", session.getRemoteAddress());
			sessions.put(session.getRemoteAddress().toString(), session);

			Logger.log(this.getClass().getSimpleName(), "Session ready: " + session.getRemoteAddress());
			break;
		case CLOSED:
			if (sessions.remove(session.getRemoteAddress().toString()) != null) {
				Logger.log(this.getClass().getSimpleName(), "Session closed: " + session.getRemoteAddress());
			} else {
				Logger.log(this.getClass().getSimpleName(), "Session closed (not found)" + session.getRemoteAddress());
			}
			break;
		}
	}
}
