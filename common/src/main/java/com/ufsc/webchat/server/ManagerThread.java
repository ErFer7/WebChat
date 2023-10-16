package com.ufsc.webchat.server;

public abstract class ManagerThread extends Thread {

	public ManagerThread() {
		super("manager-thread");
	}

	@Override public abstract void run();
}
