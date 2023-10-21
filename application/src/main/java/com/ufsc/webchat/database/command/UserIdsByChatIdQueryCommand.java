package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChatMember.chatMember;

import java.util.List;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;

public class UserIdsByChatIdQueryCommand {

	public List<Long> execute(Long chatId) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(chatMember.userId)
					.from(chatMember)
					.where(chatMember.chatId.eq(chatId))
					.fetch();
		}
	}
}
