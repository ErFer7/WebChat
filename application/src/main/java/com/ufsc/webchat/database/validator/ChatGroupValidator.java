package com.ufsc.webchat.database.validator;

import com.ufsc.webchat.database.model.UserSearchResultDto;
import com.ufsc.webchat.model.ValidationMessage;

public class ChatGroupValidator {

	public ValidationMessage validate(UserSearchResultDto userSearchResultDto) {
		if (userSearchResultDto.getFoundUsersIds().isEmpty()) {
			return new ValidationMessage("Erro ao criar grupo: nenhum dos usuários foi encontrado.", false);
		}
		var notFoundUsers = userSearchResultDto.getNotFoundUsers();
		if (!notFoundUsers.isEmpty()) {
			return new ValidationMessage("Erro ao criar grupo, usuários não encontrados: %s".formatted(notFoundUsers), false);
		}
		return new ValidationMessage(null, true);
	}
}
