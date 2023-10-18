package com.ufsc.webchat.database.model;

public class UserInfoDto {

	private Long id;
	private String passwordHash;

	public UserInfoDto(Long id, String passwordHash) {
		this.id = id;
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
