package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;
import org.snf4j.websocket.WebSocketSession;

public class Server {

	private final Handler externalHandler;
	private final Handler internalHandler;
	private final Manager managerThread;
	private final SocketChannel internalChannel;
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	public Server(Handler externalHandler, Handler internalHandler, Manager managerThread) throws IOException {
		this.externalHandler = externalHandler;
		this.internalHandler = internalHandler;
		this.managerThread = managerThread;
		this.internalChannel = internalHandler != null ? SocketChannel.open() : null;
	}

	public Server(Handler serverHandler) throws IOException {
		this(serverHandler, null, null);
	}

	public void start(int port) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();

			ServerSocketChannel externalChannel = ServerSocketChannel.open();
			externalChannel.configureBlocking(false);
			externalChannel.socket().bind(new InetSocketAddress(port));

			loop.register(externalChannel, new AbstractWebSocketSessionFactory() {
				@Override
				protected IWebSocketHandler createHandler(SocketChannel channel) {
					return Server.this.getExternalHandler();
				}
			}).sync();

			this.externalHandler.setInternalChannel(externalChannel);
			logger.info("The external handler is ready on port: {}", externalChannel.socket().getLocalPort());

			if (this.internalChannel != null) {
				this.internalChannel.configureBlocking(false);
				loop.register(this.internalChannel, new WebSocketSession(Server.this.getInternalHandler(), true));
				this.internalHandler.setInternalChannel(this.internalChannel);
			}

			if (this.managerThread != null) {
				this.managerThread.run();
			}

			loop.join();
		} catch (Exception exception) {
			logger.error("Exception: {}", exception.getMessage());
		} finally {
			loop.stop();
		}
	}

	public Handler getExternalHandler() {
		return this.externalHandler;
	}

	public Handler getInternalHandler() {
		return this.internalHandler;
	}
}
