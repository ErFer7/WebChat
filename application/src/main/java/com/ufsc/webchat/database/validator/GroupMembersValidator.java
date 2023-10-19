package com.ufsc.webchat.database.validator;

import com.ufsc.webchat.database.command.UserIdByNameCommand;
import com.ufsc.webchat.database.model.GroupUsersLists;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersValidator {
    private final UserIdByNameCommand userListByNameCommand = new UserIdByNameCommand();

    public GroupUsersLists validate(List<String> groupMembersName) {
        GroupUsersLists groupUsersLists = new GroupUsersLists();
        List<String> notFound = new ArrayList<>();
        List<Long> found = new ArrayList<>();
        for (String member : groupMembersName) {
            List<Long> userId = this.userListByNameCommand.execute(member);
            if(userId.isEmpty()) {
                notFound.add(member);
            } else {
                found.add(userId.getFirst());
            }
        }

        groupUsersLists.setFoundUsers(found);
        groupUsersLists.setNotFoundUsers(notFound);

        return groupUsersLists;
    }
}
