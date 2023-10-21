package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChat.chat;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.Chat;

public class ChatByIdQueryCommand {
    public Chat execute(Long chatId) {
        try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
            return queryFactory.createQuery()
                    .select(chat)
                    .from(chat)
                    .where(chat.id.eq(chatId))
                    .fetchOne();
        }
    }
}
