package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChat.chat;
import static com.ufsc.webchat.database.model.QChatMember.chatMember;

import com.querydsl.jpa.JPAExpressions;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;

public class ChatIdByUsersIdsQueryCommand {

	public Long execute(Long userId1, Long userId2) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(chat.id)
					.from(chat)
					.where(chat.isGroupChat.eq(Boolean.FALSE))
					.where(chat.id.in(JPAExpressions.select(chatMember.chatId)
									.from(chatMember)
									.where(chatMember.userId.eq(userId1)))
							.and(chat.id.in(JPAExpressions.select(chatMember.chatId)
									.from(chatMember)
									.where(chatMember.userId.eq(userId2)))))
					.fetchOne();
		}
	}

}
