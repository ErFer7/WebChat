package com.ufsc.webchat.database.service;

import org.json.JSONObject;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.command.MessageListByChatIdQueryCommand;
import com.ufsc.webchat.database.command.MessageSaveCommand;
import com.ufsc.webchat.database.command.UserNameByUserIdQueryCommand;
import com.ufsc.webchat.database.model.Message;
import com.ufsc.webchat.database.model.MessageCreateDto;
import com.ufsc.webchat.database.validator.AuthorizationValidator;
import com.ufsc.webchat.database.validator.MessageValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class MessageService {

	private final MessageSaveCommand messageSaveCommand = new MessageSaveCommand();
	private final MessageListByChatIdQueryCommand messageListByChatIdQueryCommand = new MessageListByChatIdQueryCommand();
	private final UserNameByUserIdQueryCommand userNameByUserIdQueryCommand = new UserNameByUserIdQueryCommand();
	private final MessageValidator messageValidator = new MessageValidator();
	private final AuthorizationValidator authorizationValidator = new AuthorizationValidator();

	public ServiceResponse saveMessage(JSONObject payload) {
		var senderId = payload.getLong("userId");
		var messageDto = new MessageCreateDto();
		messageDto.setMessage(payload.getString("message"));
		messageDto.setChatId(payload.getLong("chatId"));
		messageDto.setSenderId(senderId);

		ValidationMessage validationMessage = this.messageValidator.validate(messageDto);

		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.VALIDATION_ERROR, validationMessage.message(), null);
		}

		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			Message message = this.messageSaveCommand.execute(messageDto, em);
			transaction.commit();

			messageDto.setSentAt(message.getSentAt());
			messageDto.setId(message.getId());
			messageDto.setSenderUsername(this.userNameByUserIdQueryCommand.execute(senderId));
			return new ServiceResponse(Status.CREATED, null, messageDto);
		} catch (Exception e) {
			em.getTransaction().rollback();
			return new ServiceResponse(Status.ERROR, "Erro ao salvar mensagem no banco de dados!", null);
		}
	}

	public ServiceResponse loadMessages(JSONObject payload) {
		Long chatId = payload.getLong("chatId");
		Long userId = payload.getLong("userId");

		ValidationMessage validationMessage = this.authorizationValidator.validateUserInChat(userId, chatId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		return new ServiceResponse(Status.OK, null, this.messageListByChatIdQueryCommand.execute(chatId));
	}

}
