package com.ufsc.ine5418.protocol;

import org.json.JSONObject;

import com.ufsc.ine5418.protocol.enums.HostType;
import com.ufsc.ine5418.protocol.enums.OperationType;
import com.ufsc.ine5418.protocol.enums.PayloadType;
import com.ufsc.ine5418.protocol.enums.Status;

public class PacketFactory {

	private String host;
	private HostType hostType;
	private String token;

	public PacketFactory(HostType hostType) {
		this.hostType = hostType;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setToken(String token) {
		this.token = token;
	}

	private Packet createPacket(Status status, OperationType operationType, PayloadType payloadType, JSONObject payload) {
		return new Packet(this.host, this.hostType, this.token, status, operationType, payloadType, payload);
	}

	public Packet createGatewayConnectionRequest(String identifier, String password) {
		JSONObject payload = new JSONObject();

		payload.put("identifier", identifier);
		payload.put("password", password);

		return this.createPacket(null, OperationType.REQUEST, PayloadType.CONNECTION, payload);
	}

	public Packet createGatewayConnectionResponse(Status status) {
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.CONNECTION, null);
	}
}
