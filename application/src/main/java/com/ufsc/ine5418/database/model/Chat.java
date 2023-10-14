package com.ufsc.ine5418.database.model;

import jakarta.persistence.*;

import java.time.Instant;

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

}