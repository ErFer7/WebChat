package com.ufsc.webchat.server;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;

public class WebChatServerHandler extends ServerHandler {

	public WebChatServerHandler() {
		super(new PacketFactory(HostType.APPLICATION));
	}

	@Override
	public void readPacket(Packet packet) {
		// TODO: Implement
	}
}
