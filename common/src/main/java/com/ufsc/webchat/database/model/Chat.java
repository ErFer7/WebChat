package com.ufsc.webchat.database.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "chat")
public class Chat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "created_at", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Instant createdAt;

	@Column(name = "is_group_chat", nullable = false)
	private boolean isGroupChat;

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Instant createdAt() {
		return this.createdAt;
	}

	public boolean getIsGroupChat() {
		return this.isGroupChat;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public void setIsGroupChat(boolean isGroupChat) {
		this.isGroupChat = isGroupChat;
	}

}