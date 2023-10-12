package com.ufsc.ine5418.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.ine5418.protocol.Packet;

public abstract class WebServerHandler extends AbstractWebSocketHandler {

	protected final ConcurrentMap<Long, IStreamSession> sessions = new ConcurrentHashMap<Long, IStreamSession>();

	@Override
	public void read(Object frame) {
		if (frame instanceof TextFrame) {
			System.out.println("[Handler] Received frame: " + ((TextFrame) frame).getText());

			Packet packet = new Packet(((TextFrame) frame).getText());
			this.readPacket(packet);
		}
	}

	public abstract void readPacket(Packet packet);

	public void sendPacket(Long sessionId, Packet packet) {
		IStreamSession session = sessions.get(sessionId);
		session.writenf(new TextFrame(packet.toString()));
	}

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		switch (event) {
		case CREATED:
			System.out.println("[Handler] Session created");
			break;
		case OPENED:
			System.out.println("[Handler] Session opened");
			break;
		case READY:
			session.getAttributes().put("user-id", session.getRemoteAddress());
			sessions.put(session.getId(), session);

			System.out.println("[Handler] Session ready");
			break;
		case CLOSED:
			if (sessions.remove(session.getId()) != null) {
				System.out.println("[Handler] Session closed");
			} else {
				System.out.println("[Handler] Session closed (not found)");
			}
			break;
		case ENDING:
			System.out.println("[Handler] Session ending");
			break;
		}
	}
}
