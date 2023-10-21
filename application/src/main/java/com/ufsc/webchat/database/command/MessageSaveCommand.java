package com.ufsc.webchat.database.command;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.model.Message;
import com.ufsc.webchat.database.model.MessageCreateDto;

public class MessageSaveCommand {
	private static final Logger logger = LoggerFactory.getLogger(MessageSaveCommand.class);

	public Long execute(MessageCreateDto messageCreateDto) {
		var em = EntityManagerProvider.getEntityManager();
		var transaction = em.getTransaction();
		transaction.begin();

		Message message = new Message();
		message.setChatId(messageCreateDto.getChatId());
		message.setText(messageCreateDto.getMessage());
		message.setSenderId(messageCreateDto.getSenderId());
		message.setSentAt(Instant.now());

		em.persist(message);

		try {
			transaction.commit();
			em.close();
			return message.getId();
		} catch (Exception e) {
			logger.error("Exceção no commit no banco de dados: {}", e.getMessage());
			transaction.rollback();
			em.close();
			return null;
		}
	}
}
