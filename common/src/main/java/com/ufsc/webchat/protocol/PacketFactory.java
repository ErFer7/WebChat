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

	public Packet createOkResponse(PayloadType payloadType, String message) {
		JSONObject payload = new JSONObject();

		payload.put("message", message);

		return this.createPacket(Status.OK, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createErrorResponse(PayloadType payloadType, String message) {
		JSONObject payload = new JSONObject();

		payload.put("message", message);

		return this.createPacket(Status.ERROR, OperationType.RESPONSE, payloadType, payload);
	}

	public Packet createGatewayConnectionRequest(String identifier, String password, String host, int externalPort) {
		JSONObject payload = new JSONObject();

		payload.put("identifier", identifier);
		payload.put("password", password);
		payload.put("host", host);
		payload.put("externalPort", externalPort);

		return this.createPacket(null, OperationType.REQUEST, PayloadType.CONNECTION, payload);
	}

	public Packet createApplicationConnectionResponse(Status status, String token) {
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

	public Packet createApplicationClientRoutingResponse(Status status, Long userId, String token) {
		JSONObject payload = new JSONObject();

		payload.put("userId", userId);
		payload.put("token", token);

		return this.createPacket(status, OperationType.RESPONSE, PayloadType.ROUTING, payload);
	}

	public Packet createGatewayClientRoutingResponse(Status status, Long userId, String token, String applicationId, String applicationHost) {
		JSONObject payload = new JSONObject();

		payload.put("userId", userId);
		payload.put("token", token);
		payload.put("applicationId", applicationId);
		payload.put("applicationHost", applicationHost);

		return this.createPacket(status, OperationType.RESPONSE, PayloadType.ROUTING, payload);
	}

	public Packet createClientConnectionResponse(Status status) {
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.CONNECTION, null);
	}

	public Packet createClientRegisterUserResponse(Status status, String message) {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.USER_CREATION, payload);
	}

	public Packet createApplicationClientDisconnectingRequest(String userId) {
		JSONObject payload = new JSONObject();
		payload.put("userId", userId);
		return this.createPacket(null, OperationType.REQUEST, PayloadType.DISCONNECTION, payload);
	}

	public Packet createGatewayClientDisconnectionResponse(Long userId) {
		JSONObject payload = new JSONObject();
		payload.put("userId", userId);
		return this.createPacket(Status.OK, OperationType.RESPONSE, PayloadType.DISCONNECTION, payload);
	}

	public Packet createApplicationClientDisconnectionResponse() {
		return this.createPacket(Status.OK, OperationType.RESPONSE, PayloadType.DISCONNECTION, null);
	}

	public Packet createGroupChatAdditionResponse(Status status, String message) {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.GROUP_CHAT_ADDITION, payload);
	}

	public Packet createApplicationMessageResponse(Status status, JSONObject payload) {
		return this.createPacket(status, OperationType.RESPONSE, PayloadType.MESSAGE, payload);
	}
	
	public Packet createMessageForwarding(JSONObject payload) {
		return this.createPacket(null, OperationType.REQUEST, PayloadType.MESSAGE_FORWARDING, payload);
	}

	public Packet createGenericClientResponse(Status status, PayloadType payloadType, JSONObject payload, String message) {
		if (status.equals(Status.ERROR)) {
			return this.createErrorResponse(payloadType, message);
		}
		return this.createPacket(status, OperationType.RESPONSE, payloadType, payload);
	}
}
