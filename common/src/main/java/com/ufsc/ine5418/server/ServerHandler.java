package com.ufsc.ine5418.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.utils.Logger;

public abstract class ServerHandler extends Handler {

	protected final ConcurrentMap<Long, IStreamSession> sessions = new ConcurrentHashMap<Long, IStreamSession>();

	public abstract void readPacket(Packet packet);

	public void sendPacket(Long sessionId, Packet packet) {
		IStreamSession session = sessions.get(sessionId);
		session.writenf(new TextFrame(packet.toString()));
	}

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		switch (event) {
		case READY:
			session.getAttributes().put("user-id", session.getRemoteAddress());
			sessions.put(session.getId(), session);

			Logger.log(this.getClass().getSimpleName(), "Session ready: " + session.getRemoteAddress());
			break;
		case CLOSED:
			if (sessions.remove(session.getId()) != null) {
				Logger.log(this.getClass().getSimpleName(), "Session closed: " + session.getRemoteAddress());
			} else {
				Logger.log(this.getClass().getSimpleName(), "Session closed (not found)" + session.getRemoteAddress());
			}
			break;
		}
	}
}
