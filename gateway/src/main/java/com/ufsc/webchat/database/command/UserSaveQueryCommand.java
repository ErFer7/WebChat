package com.ufsc.webchat.database.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.model.User;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.server.PasswordHandler;

public class UserSaveQueryCommand {

	private static final Logger logger = LoggerFactory.getLogger(UserSaveQueryCommand.class);

	public boolean execute(UserDto userDto) {
		var em = EntityManagerProvider.getEntityManager();
		var transaction = em.getTransaction();
		transaction.begin();

		User user = new User();
		user.setName(userDto.getIdentifier());
		user.setPasswordHash(PasswordHandler.generatePasswordHash(userDto.getPassword()));
		em.persist(user);

		try {
			transaction.commit();
			em.close();
			return true;
		} catch (Exception e) {
			logger.error("Exceção no commit no banco de dados: {}", e.getMessage());
			transaction.rollback();
			em.close();
			return false;
		}
	}

}
