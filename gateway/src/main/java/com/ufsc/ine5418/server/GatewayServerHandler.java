package com.ufsc.ine5418.server;

import java.util.HashMap;

import org.json.JSONObject;

import com.ufsc.ine5418.protocol.Packet;
import com.ufsc.ine5418.protocol.PacketFactory;
import com.ufsc.ine5418.protocol.enums.HostType;
import com.ufsc.ine5418.protocol.enums.OperationType;
import com.ufsc.ine5418.protocol.enums.PayloadType;
import com.ufsc.ine5418.protocol.enums.Status;

public class GatewayServerHandler extends ServerHandler {

	private final String gatewayIdentifier;
	private final String gatewayPassword;
	private final HashMap<String, Integer> applicationServersClientCount;

	public GatewayServerHandler(String gatewayIdentifier, String gatewayPassword) {
		super(new PacketFactory(HostType.GATEWAY));;

		this.gatewayIdentifier = gatewayIdentifier;
		this.gatewayPassword = gatewayPassword;
		this.applicationServersClientCount = new HashMap<>();
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.APPLICATION) {
			this.processApplicationPackets(packet);
		} else if (packet.getHostType() == HostType.CLIENT) {
			this.processClientPackets(packet);
		}
	}

	// TODO: Refactor
	private void processApplicationPackets(Packet packet) {
		if (packet.getOperationType() == OperationType.REQUEST && packet.getPayloadType() == PayloadType.CONNECTION) {
			String host = packet.getHost();
			JSONObject payload = packet.getPayload();

			String identifier = payload.getString("identifier");
			String password = payload.getString("password");

			Status status = null;

			if (identifier.equals(this.gatewayIdentifier) && password.equals(this.gatewayPassword)) {
				this.applicationServersClientCount.put(host, 0);
				status = Status.OK;
			} else {
				status = Status.ERROR;
			}

			this.sendPacket(host, packetFactory.createGatewayConnectionResponse(status));
		}
	}

	private void processClientPackets(Packet packet) {
		// TODO: Implement
	}
}
