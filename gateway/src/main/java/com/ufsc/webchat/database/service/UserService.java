package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserInfoDtoByUserNameCommand;
import com.ufsc.webchat.database.command.UserSaveCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.database.validator.UserRegisterValidator;
import com.ufsc.webchat.model.ServiceAnswer;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.server.PasswordHandler;

public class UserService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();
	private final UserSaveCommand userSaveCommand = new UserSaveCommand();
	private final UserInfoDtoByUserNameCommand userInfoDtoByUserNameCommand = new UserInfoDtoByUserNameCommand();

	public ServiceAnswer register(JSONObject payload) {
		var userDto = new UserDto();
		// TODO: VERIFICAR SE PAYLOAD.GET NÃO GERA EXCEÇÕES
		userDto.setIdentifier(payload.getString("identifier"));
		userDto.setPassword(payload.getString("password"));
		ValidationMessage validationMessage = this.userRegisterValidator.validate(userDto);
		if (!validationMessage.isValid()) {
			return new ServiceAnswer(Status.ERROR, validationMessage.message());
		}
		if (!this.userSaveCommand.execute(userDto)) {
			return new ServiceAnswer(Status.ERROR, "Erro ao cadastrar usuário, tente novamente.");
		}
		return new ServiceAnswer(Status.OK, "Usuário cadastrado com sucesso!");
	}

	public Long login(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setIdentifier(payload.getString("identifier"));
		userDto.setPassword(payload.getString("password"));
		// TODO: VERIFICAR SE NÃO GERA EXCEÇÕES

		var userInfoDto = this.userInfoDtoByUserNameCommand.execute(userDto.getIdentifier());
		if (userInfoDto != null && PasswordHandler.validatePasswordWithHash(userDto.getPassword(), userInfoDto.getPasswordHash())) {
			return userInfoDto.getId();
		}
		return null;
	}
}
