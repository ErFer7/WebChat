package com.ufsc.webchat.database.model;

import java.util.List;

public class UserSearchResultDto {
	private List<Long> foundUsersIds;
	private List<String> notFoundUsers;

	public List<Long> getFoundUsersIds() {
		return this.foundUsersIds;
	}

	public void setFoundUsersIds(List<Long> foundUsersIds) {
		this.foundUsersIds = foundUsersIds;
	}

	public List<String> getNotFoundUsers() {
		return this.notFoundUsers;
	}

	public void setNotFoundUsers(List<String> notFoundUsers) {
		this.notFoundUsers = notFoundUsers;
	}
}
