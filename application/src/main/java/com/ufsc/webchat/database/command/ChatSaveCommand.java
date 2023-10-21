package com.ufsc.webchat.database.command;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.model.Chat;

public class ChatSaveCommand {
	private static final Logger logger = LoggerFactory.getLogger(ChatSaveCommand.class);

	public Long execute(String name, boolean isGroupChat) {
		var em = EntityManagerProvider.getEntityManager();
		var transaction = em.getTransaction();
		transaction.begin();

		Chat chat = new Chat();
		chat.setName(name);
		chat.setCreatedAt(Instant.now());
		chat.setIsGroupChat(isGroupChat);
		em.persist(chat);

		try {
			transaction.commit();
			em.close();
			return chat.getId();
		} catch (Exception e) {
			logger.error("Exceção no commit no banco de dados: {}", e.getMessage());
			transaction.rollback();
			em.close();
			return null;
		}
	}
}
