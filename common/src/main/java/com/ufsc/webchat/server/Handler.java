package com.ufsc.webchat.server;

import java.nio.channels.spi.AbstractSelectableChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.utils.SessionContextMap;

public abstract class Handler extends AbstractWebSocketHandler {

	protected AbstractSelectableChannel internalChannel;
	protected Thread managerThread;
	protected final SessionContextMap sessions;
	private static final Logger logger = LoggerFactory.getLogger(Handler.class);

	protected Handler() {
		this.sessions = new SessionContextMap();
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

	public void sendPacketById(String id, Packet packet) {
		try {
			IStreamSession session = this.sessions.getById(id);
			session.writenf(new TextFrame(packet.toString()));
		} catch (Exception e) {
			logger.error("Error while sending packet: {}", packet);
		}
	}

	public void sendPacketByHost(String host, Packet packet) {
		try {
			IStreamSession session = this.sessions.getByHost(host);
			session.writenf(new TextFrame(packet.toString()));
		} catch (Exception e) {
			logger.error("Error while sending packet: {}", packet);
		}
	}

	public void sendPacketBySession(IWebSocketSession session, Packet packet) {
		try {
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
		logger.info("Session created: {}", session.getName());
	}

	protected void sessionOpened(IWebSocketSession session) {
		logger.info("Session opened: {}", session.getName());
	}

	protected void sessionReady(IWebSocketSession session) {
		logger.info("Session ready: {}", session.getName());

		this.sessions.addSession(session.getName(), session.getRemoteAddress().toString(), session);
	}

	protected void sessionClosed(IWebSocketSession session) {
		logger.info("Session closed: {}", session.getName());

		this.sessions.removeByName(session.getName());
	}

	protected void sessionEnding(IWebSocketSession session) {
		logger.info("Session ending: {}", session.getName());
	}

	public void setInternalChannel(AbstractSelectableChannel internalChannel) {
		this.internalChannel = internalChannel;
	}

	public AbstractSelectableChannel getInternalChannel() {
		return this.internalChannel;
	}

	public void associateIdToHost(String host, String id) {
		this.sessions.associateToId(host, id);
	}
}
