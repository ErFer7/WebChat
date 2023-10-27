package com.ufsc.webchat.rest.model;

public class UserDto {

	private String clientId;
	private String username;
	private String password;

	public UserDto(String clientId, String username, String password) {
		this.clientId = clientId;
		this.username = username;
		this.password = password;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
