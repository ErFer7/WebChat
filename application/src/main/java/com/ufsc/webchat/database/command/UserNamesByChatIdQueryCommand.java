package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChatMember.chatMember;
import static com.ufsc.webchat.database.model.QUser.user;

import java.util.List;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;

public class UserNamesByChatIdQueryCommand {
	public List<String> execute(Long chatId) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(user.name)
					.from(chatMember)
					.where(chatMember.chatId.eq(chatId))
					.join(user).on(user.id.eq(chatMember.userId))
					.fetch();
		}
	}

}
