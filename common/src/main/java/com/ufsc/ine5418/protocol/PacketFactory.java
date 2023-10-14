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

	public PacketFactory(String host, HostType hostType) {
		this.host = host;
		this.hostType = hostType;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Packet createPacket(Status status, OperationType operationType, PayloadType payloadType, JSONObject payload) {
		return new Packet(this.host, this.hostType, this.token, status, operationType, payloadType, payload);
	}
}
