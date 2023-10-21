package com.ufsc.webchat.database.validator;

import static java.util.Objects.isNull;

import com.ufsc.webchat.database.command.ChatMemberByUserIdChatIdQueryCommand;
import com.ufsc.webchat.database.model.ChatMember;
import com.ufsc.webchat.model.ValidationMessage;

public class AuthorizationValidator {
	private final ChatMemberByUserIdChatIdQueryCommand chatMemberByUserIdChatIdCommand = new ChatMemberByUserIdChatIdQueryCommand();

	public ValidationMessage validateUserInChat(Long userId, Long chatId) {
		ChatMember currentMember = this.chatMemberByUserIdChatIdCommand.execute(userId, chatId);
		if (isNull(currentMember)) {
			return new ValidationMessage("Erro ao carregar membros: usuário atual não é membro da conversa selecionada.", false);
		}

		return new ValidationMessage(null, true);
	}

}
