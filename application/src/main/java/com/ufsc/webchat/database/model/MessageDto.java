package com.ufsc.webchat.database.model;

import java.time.Instant;

public class MessageDto {
	private Long id;
	private Long senderId;
	private String message;
	private Instant sentAt;

	public MessageDto(Long id, Long senderId, String message, Instant sentAt) {
		this.id = id;
		this.senderId = senderId;
		this.message = message;
		this.sentAt = sentAt;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSenderId() {
		return this.senderId;
	}

	public void setSenderId(Long senderId) {
		this.senderId = senderId;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Instant getSentAt() {
		return this.sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}
}
