package com.ufsc.webchat.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_member")
public class ChatMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "chat_id")
	private Long chatId;

	public Long getId() {
		return this.id;
	}

	public Long getUserId() {
		return this.userId;
	}

	public Long getChatIdId() {
		return this.chatId;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

}