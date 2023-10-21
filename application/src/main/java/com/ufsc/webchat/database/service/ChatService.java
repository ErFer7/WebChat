package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ufsc.webchat.database.command.*;
import com.ufsc.webchat.database.validator.ChatGroupAdditionValidator;
import org.json.JSONObject;

import com.ufsc.webchat.database.model.UserSearchResultDto;
import com.ufsc.webchat.database.validator.ChatGroupValidator;
import com.ufsc.webchat.model.ServiceAnswer;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class ChatService {
	private final UserIdByNameCommand userIdByNameCommand = new UserIdByNameCommand();
	private final ChatSaveCommand chatSaveCommand = new ChatSaveCommand();
	private final ChatGroupValidator chatGroupValidator = new ChatGroupValidator();
	private final ChatMemberSaveCommand chatMemberSaveCommand = new ChatMemberSaveCommand();
	private final ChatGroupAdditionValidator chatGroupAdditionValidator = new ChatGroupAdditionValidator();
	private final ChatByIdCommand chatByIdCommand = new ChatByIdCommand();
	private final ChatMemberByUserIdChatIdCommand chatMemberByUserIdChatIdCommand = new ChatMemberByUserIdChatIdCommand();

	public ServiceAnswer addToChatGroup(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");
		String addedUserName = payload.getString("addedUserName");

		Long addedUserId = this.userIdByNameCommand.execute(addedUserName);

		ValidationMessage validationMessage = this.chatGroupAdditionValidator.validate(chatId, addedUserId, userId);
		if (!validationMessage.isValid()) {
			return new ServiceAnswer(Status.ERROR, validationMessage.message());
		}

		boolean success = this.chatMemberSaveCommand.execute(chatId, addedUserId);
		if (!success) {
			return new ServiceAnswer(Status.ERROR, "Erro ao criar grupo!");
		}

		return new ServiceAnswer(Status.OK, "Usuário adicionado ao grupo com sucesso!");
	}

	public ServiceAnswer saveChatGroup(JSONObject payload) {
		// TODO: Avaliar possíveis exceções se não houver os campos no payload.
		//  Pode ser uma ideia criar um payloadValidator que avalia esses campos antes de passar pro service.
		String groupName = payload.getString("groupName");
		List<String> usernames = payload.getJSONArray("membersUsernames").toList()
				.stream()
				.map(Object::toString)
				.distinct()
				.collect(Collectors.toCollection(ArrayList::new));

		UserSearchResultDto userSearchResultDto = this.loadUsersIdFromUsernames(usernames);
		ValidationMessage validationMessage = this.chatGroupValidator.validate(userSearchResultDto);
		if (!validationMessage.isValid()) {
			return new ServiceAnswer(Status.ERROR, validationMessage.message());
		}

		List<Long> chatMembers = Stream.concat(userSearchResultDto.getFoundUsersIds().stream(), Stream.of(payload.getLong("userId")))
				.distinct().toList();
		Long chatId = this.chatSaveCommand.execute(groupName, true);

		for (Long userId : chatMembers) {
			boolean success = this.chatMemberSaveCommand.execute(chatId, userId);
			if (!success) {
				return new ServiceAnswer(Status.ERROR, "Erro ao criar grupo!");
			}
		}
		// TODO: esse erro de bd acima é bem raro, mas os commands podem ser encapsulados internamente com retries.

		return new ServiceAnswer(Status.OK, "Grupo criado com sucesso!");
	}

	private UserSearchResultDto loadUsersIdFromUsernames(List<String> usernames) {
		List<String> notFoundUsers = new ArrayList<>();
		List<Long> foundUsersIds = new ArrayList<>();
		usernames.forEach(member -> {
			Long userId = this.userIdByNameCommand.execute(member);
			if (isNull(userId)) {
				notFoundUsers.add(member);
			} else {
				foundUsersIds.add(userId);
			}
		});
		var userSearchResultDto = new UserSearchResultDto();
		userSearchResultDto.setFoundUsersIds(foundUsersIds);
		userSearchResultDto.setNotFoundUsers(notFoundUsers);
		return userSearchResultDto;
	}
}
