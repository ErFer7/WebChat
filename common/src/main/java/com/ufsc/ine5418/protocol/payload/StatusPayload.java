package com.ufsc.ine5418.protocol.payload;

import org.json.JSONObject;

public class StatusPayload extends Payload {

	private String status;

	public StatusPayload(String status) {
		this.status = status;
	}

	public StatusPayload(JSONObject jsonStatus) {
		try {
			this.status = jsonStatus.getString("status");
		} catch (Exception e) {
			System.out.println("[StatusPayload] Error parsing status: " + e.getMessage());
		}
	}

	public JSONObject toJSON() {
		JSONObject jsonStatus = new JSONObject();

		jsonStatus.put("status", this.status);

		return jsonStatus;
	}
}
