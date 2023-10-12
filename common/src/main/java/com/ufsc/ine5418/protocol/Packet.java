package com.ufsc.ine5418.protocol;

import org.json.JSONObject;

import com.ufsc.ine5418.protocol.enums.HostType;
import com.ufsc.ine5418.protocol.enums.PayloadType;
import com.ufsc.ine5418.protocol.payload.ConnectionPayload;
import com.ufsc.ine5418.protocol.payload.MessagePayload;
import com.ufsc.ine5418.protocol.payload.Payload;
import com.ufsc.ine5418.protocol.payload.StatusPayload;

public class Packet {

	private String host;
	private HostType hostType;
	private PayloadType payloadType;
	private Payload payload;

	public Packet(String host, HostType type, PayloadType payloadType, Payload payload) {
		this.host = host;
		this.hostType = type;
		this.payloadType = payloadType;
		this.payload = payload;
	}

	public Packet(String jsonString) {
		JSONObject jsonPacket = new JSONObject(jsonString);

		try {
			this.host = jsonPacket.getString("host");
			this.hostType = HostType.valueOf(jsonPacket.getString("hostType"));
			this.payloadType = PayloadType.valueOf(jsonPacket.getString("payloadType"));

			JSONObject jsonPayload = null;

			try {
				jsonPayload = jsonPacket.getJSONObject("payload");
			} catch (Exception ignored) {
				this.payload = null;
				return;
			}

			switch (this.payloadType) {
			case STATUS:
				this.payload = new StatusPayload(jsonPayload);
				break;
			case CONNECTION:
				this.payload = new ConnectionPayload(jsonPayload);
				break;
			case MESSAGE:
				this.payload = new MessagePayload(jsonPayload);
				break;
			default:
				this.payload = null;
				break;
			}
		} catch (Exception e) {
			System.out.println("[Packet] Error parsing packet: " + e.getMessage());
		}
	}

	public String toString() {
		JSONObject jsonPacket = new JSONObject();

		jsonPacket.put("host", this.host);
		jsonPacket.put("hostType", this.hostType);
		jsonPacket.put("payloadType", this.payloadType);
		jsonPacket.put("payload", this.payload != null ? this.payload.toJSON() : JSONObject.NULL);

		return jsonPacket.toString();
	}

}
