package com.ufsc.webchat.database.model;

public class MessageDto {
	private Long chatId;
	private Long senderId;
	private String message;

	public Long getChatId() {
		return this.chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
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
}
