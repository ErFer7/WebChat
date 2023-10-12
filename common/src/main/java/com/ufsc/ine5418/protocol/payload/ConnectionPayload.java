package com.ufsc.ine5418.protocol.payload;

import org.json.JSONObject;

public class ConnectionPayload extends Payload {

	private String identifier;
	private String password;

	public ConnectionPayload(String identifier, String password) {
		this.identifier = identifier;
		this.password = password;
	}

	public ConnectionPayload(JSONObject jsonPayload) {
		try {
			this.identifier = jsonPayload.getString("identifier");
			this.password = jsonPayload.getString("password");
		} catch (Exception e) {
			System.out.println("[ConnectionPayload] Error parsing payload: " + e.getMessage());
		}
	}

	public JSONObject toJSON() {
		JSONObject jsonPayload = new JSONObject();

		jsonPayload.put("identifier", this.identifier);
		jsonPayload.put("password", this.password);

		return jsonPayload;
	}
}
