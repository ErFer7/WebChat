package com.ufsc.ine5418.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;
import org.snf4j.websocket.WebSocketSession;

import com.ufsc.ine5418.utils.Logger;

public class Server {

	private final IWebSocketHandler serverHandler;
	private final IWebSocketHandler clientHandler;
	private final ServerManager manager;
	private final SocketChannel clientChannel;

	public Server(IWebSocketHandler serverHandler, IWebSocketHandler clientHandler, ServerManager manager) throws IOException {
		this.serverHandler = serverHandler;
		this.clientHandler = clientHandler;
		this.manager = manager;
		this.clientChannel = clientHandler != null ? SocketChannel.open() : null;
	}

	public Server(IWebSocketHandler serverHandler, ServerManager manager) throws IOException {
		this(serverHandler, null, manager);
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
				this.manager.setClientChannel(clientChannel);
			}

			this.manager.start();

			loop.join();
		} catch (Exception ex) {
			Logger.log(this.getClass().getSimpleName(), "Exception: " + ex.getMessage());
		} finally {
			loop.stop();
		}
	}

	public IWebSocketHandler getServerHandler() {
		return this.serverHandler;
	}

	public IWebSocketHandler getClientHandler() {
		return this.clientHandler;
	}
}
