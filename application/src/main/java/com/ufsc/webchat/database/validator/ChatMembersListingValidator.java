package com.ufsc.webchat.database.validator;

import static java.util.Objects.isNull;

import com.ufsc.webchat.database.command.ChatByIdQueryCommand;
import com.ufsc.webchat.database.command.ChatMemberByUserIdChatIdQueryCommand;
import com.ufsc.webchat.database.model.Chat;
import com.ufsc.webchat.database.model.ChatMember;
import com.ufsc.webchat.model.ValidationMessage;

public class ChatMembersListingValidator {

	private final ChatByIdQueryCommand chatByIdCommand = new ChatByIdQueryCommand();
	private final ChatMemberByUserIdChatIdQueryCommand chatMemberByUserIdChatIdCommand = new ChatMemberByUserIdChatIdQueryCommand();

	public ValidationMessage validate(Long chatId, Long userId) {
		Chat chat = this.chatByIdCommand.execute(chatId);
		if (isNull(chat)) {
			return new ValidationMessage("Erro ao carregar membros: conversa não encontrada.", false);
		}

		ChatMember currentMember = this.chatMemberByUserIdChatIdCommand.execute(userId, chatId);
		if (isNull(currentMember)) {
			return new ValidationMessage("Erro ao carregar membros: usuário atual não é membro da conversa selecionada.", false);
		}

		return new ValidationMessage(null, true);

	}
}
