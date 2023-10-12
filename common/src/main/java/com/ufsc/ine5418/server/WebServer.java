package com.ufsc.ine5418.server;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;

public class WebServer {

	private final IWebSocketHandler handler;

	public WebServer(IWebSocketHandler handler) {
		this.handler = handler;
	}

	public void start(int port) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();

			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(port));

			loop.register(channel, new AbstractWebSocketSessionFactory() {
				@Override
				protected IWebSocketHandler createHandler(SocketChannel channel) {
					return WebServer.this.getHandler();
				}
			}).sync();

			System.out.println("[Server] The server is ready on port: " + port);

			loop.join();
		} catch (Exception ex) {
			System.out.println("[Server] Exception: " + ex.getMessage());
		} finally {
			loop.stop();
		}
	}

	public IWebSocketHandler getHandler() {
		return this.handler;
	}
}
