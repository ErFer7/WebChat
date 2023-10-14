package com.ufsc.ine5418.database.model;

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
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "text", nullable = false, length = 1000)
	private String text;

	@Column(name = "sender_id")
	private Long senderId;

	@Column(name = "chat_id")
	private Long chatId;

	@Column(name = "sent_at", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Instant sentAt;

}
