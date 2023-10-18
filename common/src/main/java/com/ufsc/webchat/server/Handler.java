package com.ufsc.webchat.server;

import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.utils.OptionalKeyPairMap;

public abstract class Handler extends AbstractWebSocketHandler {

	private SocketChannel internalChannel;
	protected Thread managerThread;
	protected final OptionalKeyPairMap<String, String, IStreamSession> sessions;
	private static final Logger logger = LoggerFactory.getLogger(Handler.class);

	protected Handler() {
		this.sessions = new OptionalKeyPairMap<>();
	}

	public void setManagerThread(Thread managerThread) {
		this.managerThread = managerThread;
	}

	@Override
	public void read(Object frame) {
		if (frame instanceof TextFrame) {
			logger.info("Received frame: {}", ((TextFrame) frame).getText());

			Packet packet = new Packet(((TextFrame) frame).getText());
			this.readPacket(packet);
		}
	}

	public abstract void readPacket(Packet packet);

	public void sendPacket(String host, Packet packet) {
		try {
			IStreamSession session = this.sessions.getBySecondKey(host);
			session.writenf(new TextFrame(packet.toString()));
		} catch (Exception e) {
			logger.error("Error while sending packet: {}", packet);
		}
	}

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

	protected void sessionCreated(IWebSocketSession session) {
		logger.info("Session created: {}", session.getLocalAddress());
	}

	protected void sessionOpened(IWebSocketSession session) {
		logger.info("Session opened: {}", session.getLocalAddress());
	}

	protected void sessionReady(IWebSocketSession session) {
		logger.info("Session ready: {}", session.getLocalAddress());

		session.getAttributes().put("user-id", session.getRemoteAddress());
		this.sessions.put(session.getName(), session.getRemoteAddress().toString(), session);
	}

	protected void sessionClosed(IWebSocketSession session) {
		logger.info("Session closed: {}", session.getLocalAddress());

		this.sessions.removeByFirstKey(session.getName());
	}

	protected void sessionEnding(IWebSocketSession session) {
		logger.info("Session ending: {}", session.getLocalAddress());
	}

	public void setInternalChannel(SocketChannel internalChannel) {
		this.internalChannel = internalChannel;
	}

	public SocketChannel getInternalChannel() {
		return internalChannel;
	}
}
