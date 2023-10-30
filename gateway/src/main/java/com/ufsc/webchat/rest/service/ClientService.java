package com.ufsc.webchat.rest.service;

import org.springframework.stereotype.Service;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.rest.command.UserInfoDtoByUserNameQueryCommand;
import com.ufsc.webchat.rest.command.UserSaveCommand;
import com.ufsc.webchat.rest.model.UserDto;
import com.ufsc.webchat.rest.model.UserInfoDto;
import com.ufsc.webchat.rest.utils.PasswordHandler;
import com.ufsc.webchat.rest.validator.UserRegisterValidator;

import jakarta.persistence.EntityManager;

@Service
public class ClientService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();
	private final UserSaveCommand userSaveCommand = new UserSaveCommand();
	private final UserInfoDtoByUserNameQueryCommand userInfoDtoByUserNameCommand = new UserInfoDtoByUserNameQueryCommand();

	public Long login(UserDto userDto) {
		UserInfoDto userInfoDto = this.userInfoDtoByUserNameCommand.execute(userDto.getUsername());
		if (userInfoDto != null && PasswordHandler.validatePasswordWithHash(userDto.getPassword(), userInfoDto.getPasswordHash())) {
			return userInfoDto.getId();
		}
		return null;
	}

	public Long register(UserDto userDto) {
		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			em.getTransaction().begin();
			Long userId = this.userSaveCommand.execute(userDto, em);
			em.getTransaction().commit();
			return userId;
		} catch (Exception e) {
			em.getTransaction().rollback();
			return null;
		}
	}

}
