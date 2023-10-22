package com.ufsc.webchat.database.command;

import com.ufsc.webchat.database.model.ChatMember;

import jakarta.persistence.EntityManager;

public class ChatMemberSaveCommand {

	public void execute(Long chatId, Long userId, EntityManager em) {
		ChatMember chatMember = new ChatMember();
		chatMember.setChatId(chatId);
		chatMember.setUserId(userId);
		em.persist(chatMember);
	}

}
