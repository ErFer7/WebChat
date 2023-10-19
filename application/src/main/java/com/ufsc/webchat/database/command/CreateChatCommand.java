package com.ufsc.webchat.database.command;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.model.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CreateChatCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateChatCommand.class);

    public Long execute(String chatName, boolean isGroupChat) {
        var em = EntityManagerProvider.getEntityManager();
        var transaction = em.getTransaction();
        transaction.begin();

        Chat chat = new Chat();
        chat.setName(chatName);
        chat.setCreatedAt(Instant.now());
        chat.setIsGroupChat(isGroupChat);
        em.persist(chat);

        try {
            transaction.commit();
            em.close();
            return chat.getId();
        } catch (Exception e) {
            logger.error("Exceção no commit no banco de dados: {}", e.getMessage());
            transaction.rollback();
            em.close();
            return null;  // isso funciona?
        }
    }
}