package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChat.chat;
import static com.ufsc.webchat.database.model.QChatMember.chatMember;
import static com.ufsc.webchat.database.model.QUser.user;

import java.util.List;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.ChatDto;

public class ChatDtoListByUserIdQueryCommand {

	public List<ChatDto> execute(Long userId) {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {

			var nameExpression = Expressions.cases()
					.when(chat.isGroupChat.eq(Boolean.TRUE))
					.then(chat.name)
					.otherwise(user.name);

			return queryFactory.createQuery()
					.select(Projections.constructor(ChatDto.class, chat.id, nameExpression, chat.isGroupChat))
					.from(chat)
					.join(chatMember).on(chatMember.chatId.eq(chat.id))
					.join(user).on(user.id.eq(chatMember.userId))
					.where(chatMember.userId.eq(userId))
					.fetch();
		}
	}

}
