package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserSaveCommand;
import com.ufsc.webchat.database.model.UserDto;
import com.ufsc.webchat.database.validator.UserRegisterValidator;
import com.ufsc.webchat.protocol.enums.Status;

public class UserService {

	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();

	private final UserSaveCommand userSaveCommand = new UserSaveCommand();

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

	public Answer login(JSONObject payload) {
		var userDto = new UserDto();
		userDto.setIdentifier(payload.getString("identifier"));
		userDto.setPassword(payload.getString("password"));
		// TODO: VERIFICAR SE NÃO GERA EXCEÇÕES
		return null;
	}
}
