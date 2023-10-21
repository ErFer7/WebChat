package com.ufsc.webchat.database.command;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.Chat;

import java.util.List;

import static com.ufsc.webchat.database.model.QChat.chat;

public class ChatByIdCommand {
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
