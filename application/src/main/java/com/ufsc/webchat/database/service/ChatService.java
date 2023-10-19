package com.ufsc.webchat.database.service;

import com.ufsc.webchat.database.model.GroupUsersLists;
import com.ufsc.webchat.database.validator.GroupMembersValidator;
import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserIdByNameCommand;
import com.ufsc.webchat.protocol.enums.Status;

import java.util.List;


public class ChatService {
    private final UserIdByNameCommand userIdByNameCommand = new UserIdByNameCommand();
    private final GroupMembersValidator groupMembersValidator = new GroupMembersValidator();

    public Answer createNewGroup(List<String> usernames) {
        if (!usernames.isEmpty()){
            GroupUsersLists groupUsersLists = this.groupMembersValidator.validate(usernames);
            List<String> notFoundUsers = groupUsersLists.getNotFoundUsers();
            List<Long> foundUsers = groupUsersLists.getFoundUsers();
            if (foundUsers.isEmpty()) {
                return new Answer(Status.ERROR, "Erro ao criar grupo: nenhum dos usuários foi encontrado.");
            } else {

            }
        }
    }
}
