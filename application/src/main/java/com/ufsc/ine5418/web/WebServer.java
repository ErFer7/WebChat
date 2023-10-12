package com.ufsc.ine5418.web;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;

public class WebServer {

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
					return new WebServerHandler();
				}
			}).sync();

			System.out.println("The server is ready on port: " + port);

			loop.join();
		} catch (Exception ex) {
			System.out.println("Exception: " + ex.getMessage());
		} finally {
			loop.stop();
		}
	}
}
