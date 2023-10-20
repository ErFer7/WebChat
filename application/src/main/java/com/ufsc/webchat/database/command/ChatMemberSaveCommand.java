package com.ufsc.webchat.database.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.model.ChatMember;

public class ChatMemberSaveCommand {
	private static final Logger logger = LoggerFactory.getLogger(ChatMemberSaveCommand.class);

	public boolean execute(Long chatId, Long userId) {
		var em = EntityManagerProvider.getEntityManager();
		var transaction = em.getTransaction();
		transaction.begin();

		ChatMember chatMember = new ChatMember();
		chatMember.setChatId(chatId);
		chatMember.setUserId(userId);
		em.persist(chatMember);

		try {
			transaction.commit();
			em.close();
			return true;
		} catch (Exception e) {
			logger.error("Exceção no commit no banco de dados: {}", e.getMessage());
			transaction.rollback();
			em.close();
			return false;
		}
	}
}