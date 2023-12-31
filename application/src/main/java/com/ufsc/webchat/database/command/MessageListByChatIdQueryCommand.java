package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QMessage.message;
import static com.ufsc.webchat.database.model.QUser.user;

import java.util.List;

import com.querydsl.core.types.Projections;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.MessageDto;

public class MessageListByChatIdQueryCommand {

	public List<MessageDto> execute(Long chatId) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(Projections.constructor(MessageDto.class, message.id, message.senderId, message.text, message.sentAt, user.name))
					.from(message)
					.join(user).on(user.id.eq(message.senderId))
					.where(message.chatId.eq(chatId))
					.fetch();
		}
	}

}
