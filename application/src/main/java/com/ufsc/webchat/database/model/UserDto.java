package com.ufsc.webchat.database.model;

public class UserDto {
	private Long id;
	private String username;

	public UserDto(Long id, String username) {
		this.id = id;
		this.username = username;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
