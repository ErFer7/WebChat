package com.ufsc.webchat.protocol;

import org.json.JSONObject;

import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;

public class PacketFactory {

	private String host;
	private final HostType hostType;
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

	public Packet createGatewayConnectionResponse(Status status, String token) {
		JSONObject payload = new JSONObject();

		payload.put("token", token);

		return this.createPacket(status, OperationType.RESPONSE, PayloadType.CONNECTION, payload);
	}
}
