package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserInfoDtoByUserNameQueryCommand;
import com.ufsc.webchat.database.command.UserSaveCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.database.validator.UserRegisterValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.server.PasswordHandler;

public class UserService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();
	private final UserSaveCommand userSaveCommand = new UserSaveCommand();
	private final UserInfoDtoByUserNameQueryCommand userInfoDtoByUserNameCommand = new UserInfoDtoByUserNameQueryCommand();

	public ServiceResponse register(JSONObject payload) {
		var userDto = new UserDto();
		// TODO: VERIFICAR SE PAYLOAD.GET NÃO GERA EXCEÇÕES
		userDto.setUsername(payload.getString("username"));
		userDto.setPassword(payload.getString("password"));
		ValidationMessage validationMessage = this.userRegisterValidator.validate(userDto);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}
		if (!this.userSaveCommand.execute(userDto)) {
			return new ServiceResponse(Status.ERROR, "Erro ao cadastrar usuário, tente novamente.", null);
		}
		return new ServiceResponse(Status.CREATED, null, null);
	}

	public Long login(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setUsername(payload.getString("username"));
		userDto.setPassword(payload.getString("password"));
		// TODO: VERIFICAR SE NÃO GERA EXCEÇÕES

		var userInfoDto = this.userInfoDtoByUserNameCommand.execute(userDto.getUsername());
		if (userInfoDto != null && PasswordHandler.validatePasswordWithHash(userDto.getPassword(), userInfoDto.getPasswordHash())) {
			return userInfoDto.getId();
		}
		return null;
	}
}
