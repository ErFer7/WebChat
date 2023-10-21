package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.MessageSaveCommand;
import com.ufsc.webchat.database.model.MessageDto;
import com.ufsc.webchat.database.validator.MessageValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class MessageService {

	private final MessageSaveCommand messageSaveCommand = new MessageSaveCommand();
	private final MessageValidator messageValidator = new MessageValidator();

	public ServiceResponse saveMessage(JSONObject payload) {
		var messageDto = new MessageDto();
		messageDto.setMessage(payload.getString("message"));
		messageDto.setChatId(payload.getLong("chatId"));
		messageDto.setSenderId(payload.getLong("userId"));

		ValidationMessage validationMessage = this.messageValidator.validate(messageDto);

		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		Long messageId = this.messageSaveCommand.execute(messageDto);
		if (isNull(messageId)) {
			return new ServiceResponse(Status.ERROR, "Erro ao salvar mensagem!", null);
		}

		return new ServiceResponse(Status.OK, "Mensagem salva com sucesso!", messageId);
	}
}
