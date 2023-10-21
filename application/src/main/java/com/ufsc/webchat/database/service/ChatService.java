package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.ChatByIdQueryCommand;
import com.ufsc.webchat.database.command.ChatMemberByUserIdChatIdQueryCommand;
import com.ufsc.webchat.database.command.ChatMemberSaveQueryCommand;
import com.ufsc.webchat.database.command.ChatMembersByChatIdQueryCommand;
import com.ufsc.webchat.database.command.ChatSaveQueryCommand;
import com.ufsc.webchat.database.command.UserIdByNameQueryCommand;
import com.ufsc.webchat.database.model.UserSearchResultDto;
import com.ufsc.webchat.database.validator.ChatGroupAdditionValidator;
import com.ufsc.webchat.database.validator.ChatGroupValidator;
import com.ufsc.webchat.database.validator.ChatMembersListingValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class ChatService {
	private final UserIdByNameQueryCommand userIdByNameCommand = new UserIdByNameQueryCommand();
	private final ChatSaveQueryCommand chatSaveCommand = new ChatSaveQueryCommand();
	private final ChatGroupValidator chatGroupValidator = new ChatGroupValidator();
	private final ChatMemberSaveQueryCommand chatMemberSaveCommand = new ChatMemberSaveQueryCommand();
	private final ChatGroupAdditionValidator chatGroupAdditionValidator = new ChatGroupAdditionValidator();
	private final ChatByIdQueryCommand chatByIdCommand = new ChatByIdQueryCommand();
	private final ChatMemberByUserIdChatIdQueryCommand chatMemberByUserIdChatIdCommand = new ChatMemberByUserIdChatIdQueryCommand();
	private final ChatMembersListingValidator chatMembersListingValidator = new ChatMembersListingValidator();
	private final ChatMembersByChatIdQueryCommand chatMembersByChatIdCommand = new ChatMembersByChatIdQueryCommand();

	public ServiceResponse addToChatGroup(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");
		String addedUserName = payload.getString("addedUserName");

		Long addedUserId = this.userIdByNameCommand.execute(addedUserName);

		ValidationMessage validationMessage = this.chatGroupAdditionValidator.validate(chatId, addedUserId, userId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		boolean success = this.chatMemberSaveCommand.execute(chatId, addedUserId);
		if (!success) {
			return new ServiceResponse(Status.ERROR, "Erro ao criar grupo!", null);
		}

		return new ServiceResponse(Status.OK, "Usuário adicionado ao grupo com sucesso!", null);
	}

	public ServiceResponse saveChatGroup(JSONObject payload) {
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
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		List<Long> chatMembers = Stream.concat(userSearchResultDto.getFoundUsersIds().stream(), Stream.of(payload.getLong("userId")))
				.distinct().toList();
		Long chatId = this.chatSaveCommand.execute(groupName, true);

		for (Long userId : chatMembers) {
			boolean success = this.chatMemberSaveCommand.execute(chatId, userId);
			if (!success) {
				return new ServiceResponse(Status.ERROR, "Erro ao criar grupo!", null);
			}
		}
		// TODO: esse erro de bd acima é bem raro, mas os commands podem ser encapsulados internamente com retries.

		return new ServiceResponse(Status.OK, "Grupo criado com sucesso!", chatId);
	}

	public ServiceResponse loadUsersIdsFromChat(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");

		ValidationMessage validationMessage = this.chatMembersListingValidator.validate(chatId, userId);

		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		List<Long> chatMembersIds = this.chatMembersByChatIdCommand.execute(chatId);

		return new ServiceResponse(Status.OK, "Membros carregados com sucesso!", chatMembersIds);
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
