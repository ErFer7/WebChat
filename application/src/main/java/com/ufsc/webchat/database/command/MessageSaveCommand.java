package com.ufsc.webchat.database.command;

import java.time.Instant;

import com.ufsc.webchat.database.model.Message;
import com.ufsc.webchat.database.model.MessageCreateDto;

import jakarta.persistence.EntityManager;

public class MessageSaveCommand {

	public Message execute(MessageCreateDto messageCreateDto, EntityManager em) {
		Message message = new Message();
		message.setChatId(messageCreateDto.getChatId());
		message.setText(messageCreateDto.getMessage());
		message.setSenderId(messageCreateDto.getSenderId());
		message.setSentAt(Instant.now());

		em.persist(message);
		return message;
	}

}
