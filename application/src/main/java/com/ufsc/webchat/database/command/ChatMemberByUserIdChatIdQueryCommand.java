package com.ufsc.webchat.database.command;

import static com.ufsc.webchat.database.model.QChatMember.chatMember;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.ChatMember;

public class ChatMemberByUserIdChatIdQueryCommand {
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
