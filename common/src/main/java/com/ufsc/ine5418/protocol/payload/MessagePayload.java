package com.ufsc.ine5418.protocol.payload;

import java.util.Date;

import org.json.JSONObject;

public class MessagePayload extends Payload {

	private String username;
	private Long conversationId;
	private Date date;
	private String message;

	public MessagePayload(String username, Long conversationId, Date date, String message) {
		this.username = username;
		this.conversationId = conversationId;
		this.date = date;
		this.message = message;
	}

	public MessagePayload(JSONObject jsonPayload) {
		try {
			this.username = jsonPayload.getString("username");
			this.conversationId = jsonPayload.getLong("conversationId");
			this.date = new Date(jsonPayload.getLong("date"));
			this.message = jsonPayload.getString("message");
		} catch (Exception e) {
			System.out.println("[MessagePayload] Error parsing payload: " + e.getMessage());
		}
	}

	public JSONObject toJSON() {
		JSONObject jsonPayload = new JSONObject();

		jsonPayload.put("username", this.username);
		jsonPayload.put("conversationId", this.conversationId);
		jsonPayload.put("date", this.date.getTime());
		jsonPayload.put("message", this.message);

		return jsonPayload;
	}
}
