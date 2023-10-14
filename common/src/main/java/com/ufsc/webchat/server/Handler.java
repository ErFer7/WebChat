package com.ufsc.webchat.server;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.utils.Logger;

public abstract class Handler extends AbstractWebSocketHandler {

	private Manager manager;

	@Override
	public void read(Object frame) {
		if (frame instanceof TextFrame) {
			Logger.log(this.getClass().getSimpleName(), "Received frame: " + ((TextFrame) frame).getText());

			Packet packet = new Packet(((TextFrame) frame).getText());
			this.readPacket(packet);
		}
	}

	public abstract void readPacket(Packet packet);

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		switch (event) {
		case CREATED -> {
			this.sessionCreated(session);
		}
		case OPENED -> {
			this.sessionOpened(session);
		}
		case READY -> {
			this.sessionReady(session);
		}
		case CLOSED -> {
			this.sessionClosed(session);
		}
		case ENDING -> {
			this.sessionEnding(session);
		}
		}
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	public Manager getManager() {
		return manager;
	}

	protected void sessionCreated(IWebSocketSession session) {
		Logger.log(this.getClass().getSimpleName(), "Session created: " + session.getLocalAddress());
	}

	protected void sessionOpened(IWebSocketSession session) {
		Logger.log(this.getClass().getSimpleName(), "Session opened: " + session.getLocalAddress());
	}

	protected void sessionReady(IWebSocketSession session) {
		Logger.log(this.getClass().getSimpleName(), "Session ready: " + session.getLocalAddress());
	}

	protected void sessionClosed(IWebSocketSession session) {
		Logger.log(this.getClass().getSimpleName(), "Session closed: " + session.getLocalAddress());
	}

	protected void sessionEnding(IWebSocketSession session) {
		Logger.log(this.getClass().getSimpleName(), "Session ending: " + session.getLocalAddress());
	}
}
