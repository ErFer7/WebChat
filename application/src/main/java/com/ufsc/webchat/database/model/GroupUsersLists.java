package com.ufsc.webchat.database.model;

import java.util.List;

public class GroupUsersLists {
    List<Long> foundUsers;
    List<String> notFoundUsers;

    public List<Long> getFoundUsers() {
        return this.foundUsers;
    }

    public void setFoundUsers(List<Long> foundUsers) {
        this.foundUsers = foundUsers;
    }

    public List<String> getNotFoundUsers() {
        return this.notFoundUsers;
    }

    public void setNotFoundUsers(List<String> notFoundUsers) {
        this.notFoundUsers = notFoundUsers;
    }
}
