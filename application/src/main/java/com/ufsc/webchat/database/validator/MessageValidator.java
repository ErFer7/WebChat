package com.ufsc.webchat.database.validator;

import static java.util.Objects.isNull;

import com.ufsc.webchat.database.command.ChatByIdQueryCommand;
import com.ufsc.webchat.database.model.MessageCreateDto;
import com.ufsc.webchat.model.ValidationMessage;

public class MessageValidator {
	private final Integer MESSAGE_MAX_LENGTH = 1000;
	private final ChatByIdQueryCommand chatByIdQueryCommand = new ChatByIdQueryCommand();

	public ValidationMessage validate(MessageCreateDto messageCreateDto) {
		var message = messageCreateDto.getMessage();
		var chatId = messageCreateDto.getChatId();

		if (isNull(message) || isNull(chatId)) {
			return new ValidationMessage("Erro ao criar mensagem: campos obrigatórios não preenchidos.", false);
		}
		if (message.isBlank()) {
			return new ValidationMessage("Erro ao criar mensagem: mensagem vazia.", false);
		}
		if (message.length() > this.MESSAGE_MAX_LENGTH) {
			return new ValidationMessage("Erro ao criar mensagem: mensagem muito longa.", false);
		}
		if (isNull(this.chatByIdQueryCommand.execute(chatId))) {
			return new ValidationMessage("Erro ao criar mensagem: chat não existe.", false);
		}

		return new ValidationMessage(null, true);
	}

}
