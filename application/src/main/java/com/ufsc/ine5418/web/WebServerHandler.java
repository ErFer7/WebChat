package com.ufsc.ine5418.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.TextFrame;

public class WebServerHandler extends AbstractWebSocketHandler {

	private final static ConcurrentMap<Long, IStreamSession> sessions = new ConcurrentHashMap<Long, IStreamSession>();

	@Override
	public void read(Object frame) {
		if (frame instanceof Frame) {
			System.out.println("[Handler] Received frame: " + ((TextFrame) frame).getText());
			send("Received frame: " + ((TextFrame) frame).getText());
		}
	}

	void send(String message) {
		for (Long sessionId : sessions.keySet()) {
			IStreamSession session = sessions.get(sessionId);
			session.writenf(new TextFrame("Received frame: " + message));
		}
	}

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) getSession();

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
			}
			break;
		case ENDING:
			System.out.println("[Handler] Session ending");
			break;
		}
	}
}
