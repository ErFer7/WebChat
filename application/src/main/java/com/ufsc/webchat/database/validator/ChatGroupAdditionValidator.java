package com.ufsc.webchat.database.validator;

import static java.util.Objects.isNull;

import com.ufsc.webchat.database.command.ChatByIdQueryCommand;
import com.ufsc.webchat.database.command.ChatMemberByUserIdChatIdQueryCommand;
import com.ufsc.webchat.database.model.Chat;
import com.ufsc.webchat.database.model.ChatMember;
import com.ufsc.webchat.model.ValidationMessage;

public class ChatGroupAdditionValidator {

	private final ChatByIdQueryCommand chatByIdCommand = new ChatByIdQueryCommand();
	private final ChatMemberByUserIdChatIdQueryCommand chatMemberByUserIdChatIdCommand = new ChatMemberByUserIdChatIdQueryCommand();

	public ValidationMessage validate(Long chatId, Long addedUserId, Long userId) {
		if (isNull(addedUserId)) {
			return new ValidationMessage("Erro ao adicionar ao grupo: usuário não encontrado.", false);
		}

		Chat chat = this.chatByIdCommand.execute(chatId);
		if (isNull(chat)) {
			return new ValidationMessage("Erro ao adicionar ao grupo: grupo não encontrado.", false);
		}

		if (!chat.getIsGroupChat()) {
			return new ValidationMessage("Erro ao adicionar ao chat: não é um grupo.", false);
		}

		ChatMember currentMember = this.chatMemberByUserIdChatIdCommand.execute(userId, chatId);
		if (isNull(currentMember)) {
			return new ValidationMessage("Erro ao adicionar ao grupo: usuário atual não é membro do grupo selecionado.", false);
		}

		ChatMember newMember = this.chatMemberByUserIdChatIdCommand.execute(addedUserId, chatId);
		if (!isNull(newMember)) {
			return new ValidationMessage("Erro ao adicionar ao grupo: usuário já é membro do grupo selecionado.", false);
		}

		return new ValidationMessage(null, true);

	}
}
