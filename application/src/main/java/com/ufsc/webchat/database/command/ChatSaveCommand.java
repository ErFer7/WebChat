package com.ufsc.webchat.database.command;

import java.time.Instant;

import com.ufsc.webchat.database.model.Chat;

import jakarta.persistence.EntityManager;

public class ChatSaveCommand {

	public Long execute(String name, boolean isGroupChat, EntityManager em) {
		Chat chat = new Chat();
		chat.setName(name);
		chat.setCreatedAt(Instant.now());
		chat.setIsGroupChat(isGroupChat);
		em.persist(chat);
		return chat.getId();
	}

}
