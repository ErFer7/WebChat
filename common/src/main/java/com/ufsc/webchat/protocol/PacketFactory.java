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

	public String getHost() {
		return this.host;
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

	public Packet createClientRoutingRequest(Long userId, String token) {
		JSONObject payload = new JSONObject();

		payload.put("userId", userId);
		payload.put("token", token);

		return this.createPacket(null, OperationType.REQUEST, PayloadType.ROUTING, payload);
	}

	public Packet createClientRoutingResponse(Status status, Long userId, String token) {
		JSONObject payload = new JSONObject();

		payload.put("userId", userId);
		payload.put("token", token);

		return this.createPacket(status, OperationType.RESPONSE, PayloadType.ROUTING, payload);
	}

	public Packet createClientRegisterUserResponse(Status status, String message) {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.REGISTER_USER, payload);
	}

	public Packet createClientLoginErrorResponse() {
		JSONObject payload = new JSONObject();
		payload.put("message", "Falha no login, usuário não encontrado ou senha incorreta.");
		return this.createPacket(Status.ERROR, OperationType.RESPONSE, PayloadType.ROUTING, payload);
	}

}
