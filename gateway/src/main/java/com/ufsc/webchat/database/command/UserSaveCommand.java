package com.ufsc.webchat.database.command;

import com.ufsc.webchat.database.model.User;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.server.PasswordHandler;

import jakarta.persistence.EntityManager;

public class UserSaveCommand {

	public void execute(UserDto userDto, EntityManager em) {
		User user = new User();
		user.setName(userDto.getUsername());
		user.setPasswordHash(PasswordHandler.generatePasswordHash(userDto.getPassword()));
		em.persist(user);
	}

}
