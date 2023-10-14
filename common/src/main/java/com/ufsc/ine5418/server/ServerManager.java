package com.ufsc.ine5418.server;

import java.nio.channels.SocketChannel;

public abstract class ServerManager extends Thread {

	protected SocketChannel clientChannel;

	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}

	public abstract void run();
}
