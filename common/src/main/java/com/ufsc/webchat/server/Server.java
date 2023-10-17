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

	private final ServerHandler serverHandler;
	private final ClientHandler clientHandler;
	private final Thread managerThread;
	private final SocketChannel clientChannel;
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	public Server(ServerHandler serverHandler, ClientHandler clientHandler, Thread managerThread) throws IOException {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.managerThread = managerThread;
		this.clientChannel = clientHandler != null ? SocketChannel.open() : null;
	}

	public Server(ServerHandler serverHandler) throws IOException {
		this(serverHandler, null, null);
	}

	public void start(int port) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();

			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port));

			loop.register(serverChannel, new AbstractWebSocketSessionFactory() {
				@Override
				protected IWebSocketHandler createHandler(SocketChannel channel) {
					return Server.this.getServerHandler();
				}
			}).sync();

			logger.info("The server is ready on port: {}", port);

			if (this.clientChannel != null) {
				this.clientChannel.configureBlocking(false);
				loop.register(this.clientChannel, new WebSocketSession(Server.this.getClientHandler(), true));
				this.clientHandler.setClientChannel(this.clientChannel);
			}

			if (this.managerThread != null) {
				this.managerThread.start();
			}

			loop.join();
		} catch (Exception exception) {
			logger.error("Exception: {}", exception.getMessage());
		} finally {
			loop.stop();
		}
	}

	public ServerHandler getServerHandler() {
		return this.serverHandler;
	}

	public ClientHandler getClientHandler() {
		return this.clientHandler;
	}
}
