package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.command.UserInfoDtoByUserNameQueryCommand;
import com.ufsc.webchat.database.command.UserSaveCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.database.validator.UserRegisterValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.server.PasswordHandler;

import jakarta.persistence.EntityManager;

public class UserService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();
	private final UserSaveCommand userSaveCommand = new UserSaveCommand();
	private final UserInfoDtoByUserNameQueryCommand userInfoDtoByUserNameCommand = new UserInfoDtoByUserNameQueryCommand();

	public ServiceResponse register(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setUsername(payload.getString("username"));
		userDto.setPassword(payload.getString("password"));
		ValidationMessage validationMessage = this.userRegisterValidator.validate(userDto);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			em.getTransaction().begin();
			this.userSaveCommand.execute(userDto, em);
			em.getTransaction().commit();
			return new ServiceResponse(Status.CREATED, null, null);
		} catch (Exception e) {
			em.getTransaction().rollback();
			return new ServiceResponse(Status.ERROR, "Erro ao cadastrar usuário, tente novamente.", null);
		}
	}

	public Long login(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setUsername(payload.getString("username"));
		userDto.setPassword(payload.getString("password"));

		var userInfoDto = this.userInfoDtoByUserNameCommand.execute(userDto.getUsername());
		if (userInfoDto != null && PasswordHandler.validatePasswordWithHash(userDto.getPassword(), userInfoDto.getPasswordHash())) {
			return userInfoDto.getId();
		}
		return null;
	}
}
