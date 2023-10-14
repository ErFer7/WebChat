package com.ufsc.webchat.protocol;

import org.json.JSONObject;

import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.protocol.enums.OperationType;
import com.ufsc.webchat.protocol.enums.PayloadType;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.protocol.utils.FieldParser;
import com.ufsc.webchat.utils.Logger;

public class Packet {

	private String host;
	private HostType hostType;
	private String token;
	private Status status;
	private OperationType operationType;
	private PayloadType payloadType;
	private JSONObject payload;

	public Packet(String host, HostType type, String token, Status status, OperationType operationType, PayloadType payloadType, JSONObject payload) {
		this.host = host;
		this.hostType = type;
		this.token = token;
		this.status = status;
		this.operationType = operationType;
		this.payloadType = payloadType;
		this.payload = payload;
	}

	public Packet(String jsonString) {
		JSONObject jsonPacket = new JSONObject(jsonString);

		try {
			this.host = jsonPacket.getString("host");
			this.hostType = HostType.valueOf(jsonPacket.getString("hostType"));
			this.token = FieldParser.nullableFieldToString(jsonPacket, "token");

			String status = FieldParser.nullableFieldToString(jsonPacket, "status");
			this.status = status != null ? Status.valueOf(status) : null;

			this.operationType = OperationType.valueOf(jsonPacket.getString("operationType"));
			this.payloadType = PayloadType.valueOf(jsonPacket.getString("payloadType"));
			this.payload = FieldParser.nullableFieldToJSONObject(jsonPacket, "payload");
		} catch (Exception exception) {
			Logger.log(this.getClass().getSimpleName(), "Error parsing packet: " + exception.getMessage());
		}
	}

	public String toString() {
		JSONObject jsonPacket = new JSONObject();

		jsonPacket.put("host", this.host);
		jsonPacket.put("hostType", this.hostType);
		jsonPacket.put("token", this.token != null ? this.token : JSONObject.NULL);
		jsonPacket.put("status", this.status != null ? this.status : JSONObject.NULL);
		jsonPacket.put("operationType", this.operationType);
		jsonPacket.put("payloadType", this.payloadType);
		jsonPacket.put("payload", this.payload != null ? this.payload : JSONObject.NULL);

		return jsonPacket.toString();
	}

	public String getHost() {
		return host;
	}

	public HostType getHostType() {
		return hostType;
	}

	public String getToken() {
		return token;
	}

	public Status getStatus() {
		return status;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public PayloadType getPayloadType() {
		return payloadType;
	}

	public JSONObject getPayload() {
		return payload;
	}
}
