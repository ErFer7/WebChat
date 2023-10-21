package com.ufsc.webchat.database.command;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.Chat;
import com.ufsc.webchat.database.model.ChatMember;

import static com.ufsc.webchat.database.model.QChatMember.chatMember;

public class ChatMemberByUserIdChatIdCommand {
    public ChatMember execute(Long userId, Long chatId) {
        try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
            return queryFactory.createQuery()
                    .select(chatMember)
                    .from(chatMember)
                    .where(chatMember.userId.eq(userId).and(chatMember.chatId.eq(chatId)))
                    .fetchOne();
        }
    }
}
