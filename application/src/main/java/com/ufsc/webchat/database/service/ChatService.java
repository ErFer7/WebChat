package com.ufsc.webchat.database.service;

import com.ufsc.webchat.database.command.AddChatMember;
import com.ufsc.webchat.database.command.CreateChatCommand;
import com.ufsc.webchat.database.model.GroupUsersLists;
import com.ufsc.webchat.database.validator.GroupMembersValidator;
import org.json.JSONObject;

import com.ufsc.webchat.database.command.UserIdByNameCommand;
import com.ufsc.webchat.protocol.enums.Status;

import java.util.List;


public class ChatService {
    private final UserIdByNameCommand userIdByNameCommand = new UserIdByNameCommand();
    private final CreateChatCommand createChatCommand = new CreateChatCommand();
    private final GroupMembersValidator groupMembersValidator = new GroupMembersValidator();
    private final AddChatMember addChatMember = new AddChatMember();

    public Answer createNewGroup(List<String> usernames, String groupName) {
        if (!usernames.isEmpty()){
            GroupUsersLists groupUsersLists = this.groupMembersValidator.validate(usernames);
            List<String> notFoundUsers = groupUsersLists.getNotFoundUsers();
            List<Long> foundUsers = groupUsersLists.getFoundUsers();
            if (foundUsers.isEmpty()) {
                return new Answer(Status.ERROR, "Erro ao criar grupo: nenhum dos usuários foi encontrado.");
            } else {
                Long groupId = this.createChatCommand.execute(groupName, true);
                for (Long user : foundUsers) {
                    boolean success = this.addChatMember.execute(groupId, user);
                    if (!success) {
                        return new Answer(Status.ERROR, "Erro ao criar grupo!");
                    }
                }
                if (notFoundUsers.isEmpty()) {
                    return new Answer(Status.OK, "Grupo criado com sucesso!");
                } else {
                    String notFoundMessage = String.join(", ", notFoundUsers);
                    return new Answer(Status.OK, "Grupo criado! AVISO - USUÁRIOS NÃO ENCONTRADOS: " + notFoundMessage);
                }
            }
        } else {
            return new Answer(Status.ERROR, "Erro ao criar grupo: nenhum usuário informado.");
        }
    }
}
