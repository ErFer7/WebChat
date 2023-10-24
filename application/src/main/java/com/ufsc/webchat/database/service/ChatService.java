package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.command.ChatDtoListByUserIdQueryCommand;
import com.ufsc.webchat.database.command.ChatIdByUsersIdsQueryCommand;
import com.ufsc.webchat.database.command.ChatMemberSaveCommand;
import com.ufsc.webchat.database.command.ChatSaveCommand;
import com.ufsc.webchat.database.model.ChatDto;
import com.ufsc.webchat.database.model.UserSearchResultDto;
import com.ufsc.webchat.database.validator.ChatGroupAdditionValidator;
import com.ufsc.webchat.database.validator.ChatGroupValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class ChatService {

	private final UserService userService = new UserService();
	private final ChatGroupValidator chatGroupValidator = new ChatGroupValidator();
	private final ChatGroupAdditionValidator chatGroupAdditionValidator = new ChatGroupAdditionValidator();
	private final ChatSaveCommand chatSaveCommand = new ChatSaveCommand();
	private final ChatMemberSaveCommand chatMemberSaveCommand = new ChatMemberSaveCommand();
	private final ChatIdByUsersIdsQueryCommand chatIdByUsersIdsQueryCommand = new ChatIdByUsersIdsQueryCommand();
	private final ChatDtoListByUserIdQueryCommand chatDtoListByUserIdQueryCommand = new ChatDtoListByUserIdQueryCommand();

	public ServiceResponse addToChatGroup(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");
		String addedUserName = payload.getString("addedUserName");

		Long addedUserId = this.userService.loadUserIdByName(addedUserName);

		ValidationMessage validationMessage = this.chatGroupAdditionValidator.validate(chatId, addedUserId, userId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			this.chatMemberSaveCommand.execute(chatId, addedUserId, em);
			transaction.commit();
			return new ServiceResponse(Status.OK, "Usuário adicionado ao grupo com sucesso!", null);
		} catch (Exception e) {
			em.getTransaction().rollback();
			return new ServiceResponse(Status.ERROR, "Erro ao adicionar no grupo!", null);
		}

	}

	public ServiceResponse saveChatGroup(JSONObject payload) {
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

		List<Long> chatMembers = userSearchResultDto.getFoundUsersIds();
		Long userId = payload.getLong("userId");

		if (!chatMembers.contains(userId)) {
			chatMembers.add(userId);
		}

		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			Long chatId = this.chatSaveCommand.execute(groupName, true, em);
			for (Long memberId : chatMembers) {
				this.chatMemberSaveCommand.execute(chatId, memberId, em);
			}
			transaction.commit();
			return new ServiceResponse(Status.CREATED, null, chatId);
		} catch (Exception e) {
			em.getTransaction().rollback();
			return new ServiceResponse(Status.ERROR, "Erro ao criar grupo!", null);
		}
	}

	public ServiceResponse loadOrSaveChatIdByUsers(JSONObject payload) {
		Long userId = payload.getLong("userId");
		String targetUsername = payload.getString("targetUsername");
		Long targetUserId = this.userService.loadUserIdByName(targetUsername);
		if (isNull(targetUserId)) {
			return new ServiceResponse(Status.ERROR, "Usuário não encontrado!", null);
		}

		Long chatId = this.chatIdByUsersIdsQueryCommand.execute(userId, targetUserId);
		if (!isNull(chatId)) {
			return new ServiceResponse(Status.OK, null, chatId);
		}

		return this.saveChatOneToOne(userId, targetUserId);
	}

	private ServiceResponse saveChatOneToOne(Long userId1, Long userId2) {
		EntityManager em = EntityManagerProvider.getEntityManager();
		try (em) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			Long newChatId = this.chatSaveCommand.execute(null, false, em);
			this.chatMemberSaveCommand.execute(newChatId, userId1, em);
			if (!Objects.equals(userId1, userId2)) {
				this.chatMemberSaveCommand.execute(newChatId, userId2, em);
			}
			transaction.commit();
			return new ServiceResponse(Status.CREATED, null, newChatId);
		} catch (Exception e) {
			em.getTransaction().rollback();
			return new ServiceResponse(Status.ERROR, "Erro ao criar chat!", null);
		}
	}

	public List<ChatDto> loadChatDtoListByUserId(JSONObject payload) {
		return this.chatDtoListByUserIdQueryCommand.execute(payload.getLong("userId"));
	}

	private UserSearchResultDto loadUsersIdFromUsernames(List<String> usernames) {
		List<String> notFoundUsers = new ArrayList<>();
		List<Long> foundUsersIds = new ArrayList<>();
		usernames.forEach(member -> {
			Long userId = this.userService.loadUserIdByName(member);
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
