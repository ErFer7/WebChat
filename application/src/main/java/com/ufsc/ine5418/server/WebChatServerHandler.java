package com.ufsc.ine5418.server;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.protocol.PacketFactory;
import com.ufsc.ine5418.protocol.enums.HostType;

public class WebChatServerHandler extends ServerHandler {

	public WebChatServerHandler() {
		super(new PacketFactory(HostType.APPLICATION));
	}

	@Override
	public void readPacket(Packet packet) {
		// TODO: Implement
	}
}
