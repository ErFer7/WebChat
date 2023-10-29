package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserDtoListQueryCommand;
import com.ufsc.webchat.database.command.UserIdByNameQueryCommand;
import com.ufsc.webchat.database.command.UserIdsByChatIdQueryCommand;
import com.ufsc.webchat.database.command.UserNamesByChatIdQueryCommand;
import com.ufsc.webchat.database.validator.AuthorizationValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class UserService {
	private final UserIdByNameQueryCommand userIdByNameCommand = new UserIdByNameQueryCommand();
	private final UserIdsByChatIdQueryCommand userIdsByChatIdQueryCommand = new UserIdsByChatIdQueryCommand();
	private final UserDtoListQueryCommand userDtoListQueryCommand = new UserDtoListQueryCommand();
	private final UserNamesByChatIdQueryCommand userNamesByChatIdQueryCommand = new UserNamesByChatIdQueryCommand();

	private final AuthorizationValidator authorizationValidator = new AuthorizationValidator();

	public ServiceResponse loadUsersIdsFromChat(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		ValidationMessage validationMessage = this.authorizationValidator.validateUserInChat(userId, chatId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.VALIDATION_ERROR, validationMessage.message(), null);
		}

		return new ServiceResponse(Status.OK, null, this.userIdsByChatIdQueryCommand.execute(chatId));
	}

	public ServiceResponse loadUserNamesFromChatId(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		ValidationMessage validationMessage = this.authorizationValidator.validateUserInChat(userId, chatId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.VALIDATION_ERROR, validationMessage.message(), null);
		}

		return new ServiceResponse(Status.OK, null, this.userNamesByChatIdQueryCommand.execute(chatId));
	}

	public ServiceResponse loadUserDtoList() {
		return new ServiceResponse(Status.OK, null, this.userDtoListQueryCommand.execute());
	}

	public Long loadUserIdByName(String username) {
		return this.userIdByNameCommand.execute(username);
	}

}
