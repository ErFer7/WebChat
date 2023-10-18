package com.ufsc.webchat.server;

import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.enums.HostType;

public class ExternalHandler extends Handler {

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.CLIENT) {
			((ManagerThread) this.managerThread).processClientPackets(packet);
		} else if (packet.getHostType() == HostType.APPLICATION) {
			//
		}
	}
}
