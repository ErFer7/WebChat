package com.ufsc.webchat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;

public abstract class Handler extends AbstractWebSocketHandler {

	private Thread managerThread;

	private static final Logger logger = LoggerFactory.getLogger(Handler.class);

	@Override
	public void read(Object frame) {
		if (frame instanceof TextFrame) {
			logger.info("Received frame: {}", ((TextFrame) frame).getText());

			Packet packet = new Packet(((TextFrame) frame).getText());
			this.readPacket(packet);
		}
	}

	public abstract void readPacket(Packet packet);

	@Override
	public void event(SessionEvent event) {
		IWebSocketSession session = (IWebSocketSession) this.getSession();

		switch (event) {
		case CREATED -> this.sessionCreated(session);
		case OPENED -> this.sessionOpened(session);
		case READY -> this.sessionReady(session);
		case CLOSED -> this.sessionClosed(session);
		case ENDING -> this.sessionEnding(session);
		}
	}

	public void setManager(Thread managerThread) {
		this.managerThread = managerThread;
	}

	public Thread getManager() {
		return this.managerThread;
	}

	protected void sessionCreated(IWebSocketSession session) {
		logger.info("Session created: {}", session.getLocalAddress());
	}

	protected void sessionOpened(IWebSocketSession session) {
		logger.info("Session opened: {}", session.getLocalAddress());
	}

	protected void sessionReady(IWebSocketSession session) {
		logger.info("Session ready: {}", session.getLocalAddress());
	}

	protected void sessionClosed(IWebSocketSession session) {
		logger.info("Session closed: {}", session.getLocalAddress());
	}

	protected void sessionEnding(IWebSocketSession session) {
		logger.info("Session ending: {}", session.getLocalAddress());
	}
}
