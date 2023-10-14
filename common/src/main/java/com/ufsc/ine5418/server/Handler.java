package com.ufsc.ine5418.server;

import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.frame.TextFrame;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.utils.Logger;

public abstract class Handler extends AbstractWebSocketHandler {

	@Override
	public void read(Object frame) {
		if (frame instanceof TextFrame) {
			Logger.log(this.getClass().getSimpleName(), "Received frame: " + ((TextFrame) frame).getText());

			Packet packet = new Packet(((TextFrame) frame).getText());
			this.readPacket(packet);
		}
	}

	public abstract void readPacket(Packet packet);
}
