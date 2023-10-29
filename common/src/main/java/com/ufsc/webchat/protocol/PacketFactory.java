package com.ufsc.webchat.protocol;

import org.json.JSONObject;

import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;

public class PacketFactory {

	private String id;
	private final HostType hostType;
	private String token;

	public PacketFactory(String id, HostType hostType) {
		this.id = id;
		this.hostType = hostType;
	}

	public String getId() {
		return this.id;
	}

	public void setToken(String token) {
		this.token = token;
	}

	private Packet createPacket(Status status, OperationType operationType, PayloadType payloadType, JSONObject payload) {
		return new Packet(this.id, this.hostType, this.token, status, operationType, payloadType, payload);
	}

	public Packet createHandshakeInfo(String host) {
		JSONObject payload = new JSONObject();

		payload.put("host", host);

		return this.createPacket(Status.OK, OperationType.INFO, PayloadType.HOST, payload);
	}

	public Packet createRequest(PayloadType payloadType, JSONObject payload) {
		return this.createPacket(null, OperationType.REQUEST, payloadType, payload);
	}

	public Packet createOkResponse(PayloadType payloadType, String message) {
		JSONObject payload = new JSONObject();

		payload.put("message", message);

		return this.createPacket(Status.OK, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createOkResponse(PayloadType payloadType, JSONObject payload) {
		return this.createPacket(Status.OK, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createErrorResponse(PayloadType payloadType, String message) {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		return this.createPacket(Status.ERROR, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createValidationErrorResponse(PayloadType payloadType, String message) {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		return this.createPacket(Status.VALIDATION_ERROR, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createGenericClientResponse(Status status, PayloadType payloadType, JSONObject payload, String message) {
		if (status.equals(Status.ERROR)) {
			return this.createErrorResponse(payloadType, message);
		} else if (status.equals(Status.VALIDATION_ERROR)) {
			return this.createValidationErrorResponse(payloadType, message);
		}
		return this.createPacket(status, OperationType.RESPONSE, payloadType, payload);
	}
}
