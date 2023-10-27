package com.ufsc.webchat.rest.command;

import com.ufsc.webchat.database.model.User;
import com.ufsc.webchat.rest.model.UserDto;
import com.ufsc.webchat.rest.utils.PasswordHandler;

import jakarta.persistence.EntityManager;

public class UserSaveCommand {

	public Long execute(UserDto userDto, EntityManager em) {
		User user = new User();
		user.setName(userDto.getUsername());
		user.setPasswordHash(PasswordHandler.generatePasswordHash(userDto.getPassword()));
		em.persist(user);
		return user.getId();
	}

}
