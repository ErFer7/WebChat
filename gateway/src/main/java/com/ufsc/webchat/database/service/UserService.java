package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserInfoDtoByUserNameCommand;
import com.ufsc.webchat.database.command.UserSaveCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.database.validator.UserRegisterValidator;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.server.PasswordHandler;

public class UserService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();
	private final UserSaveCommand userSaveCommand = new UserSaveCommand();
	private final UserInfoDtoByUserNameCommand userInfoDtoByUserNameCommand = new UserInfoDtoByUserNameCommand();

	public Answer register(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setIdentifier(payload.getString("identifier"));
		userDto.setPassword(payload.getString("password"));
		// TODO: VERIFICAR SE NÃO GERA EXCEÇÕES

		boolean isValid = this.userRegisterValidator.validate(userDto);

		if (isValid) {
			if (this.userSaveCommand.execute(userDto)) {
				return new Answer(Status.OK, "Usuário cadastrado com sucesso!");
			} else {
				return new Answer(Status.ERROR, "Erro ao cadastrar usuário, tente novamente.");
			}
		} else {
			return new Answer(Status.ERROR, "Informações inválidas: ou estão nulas, ou usuário já existe com o identificador informado.");
		}
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
