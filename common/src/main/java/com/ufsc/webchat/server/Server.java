package com.ufsc.webchat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;
import org.snf4j.websocket.WebSocketSession;

import com.ufsc.webchat.utils.Logger;

public class Server {

	private final ServerHandler serverHandler;
	private final ClientHandler clientHandler;
	private final Manager manager;
	private final SocketChannel clientChannel;

	public Server(ServerHandler serverHandler, ClientHandler clientHandler, Manager manager) throws IOException {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.manager = manager;
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

			Logger.log(this.getClass().getSimpleName(), "The server is ready on port: " + port);

			if (this.clientChannel != null) {
				this.clientChannel.configureBlocking(false);
				loop.register(clientChannel, new WebSocketSession(Server.this.getClientHandler(), true));
				this.clientHandler.setClientChannel(clientChannel);
			}

			if (this.manager != null) {
				this.manager.start();
			}

			loop.join();
		} catch (Exception exception) {
			Logger.log(this.getClass().getSimpleName(), "Exception: " + exception.getMessage());
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
