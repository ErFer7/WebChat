package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserIdByNameQueryCommand;
import com.ufsc.webchat.database.command.UserIdsByChatIdQueryCommand;
import com.ufsc.webchat.database.validator.ChatMembersListingValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class UserService {
	private final ChatMembersListingValidator chatMembersListingValidator = new ChatMembersListingValidator();
	private final UserIdByNameQueryCommand userIdByNameCommand = new UserIdByNameQueryCommand();
	private final UserIdsByChatIdQueryCommand userIdsByChatIdQueryCommand = new UserIdsByChatIdQueryCommand();

	public ServiceResponse loadUsersIdsFromChat(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		ValidationMessage validationMessage = this.chatMembersListingValidator.validate(chatId, userId);

		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}
		return new ServiceResponse(Status.OK, "Membros carregados com sucesso!", this.userIdsByChatIdQueryCommand.execute(chatId));
	}

	public Long loadUserIdByName(String username) {
		return this.userIdByNameCommand.execute(username);
	}

}
