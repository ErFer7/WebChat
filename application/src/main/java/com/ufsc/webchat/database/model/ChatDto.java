package com.ufsc.webchat.database.model;

public class ChatDto {

	private Long id;
	private String name;
	private boolean isGroupChat;

	public ChatDto(Long id, String name, boolean isGroupChat) {
		this.id = id;
		this.name = name;
		this.isGroupChat = isGroupChat;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isGroupChat() {
		return this.isGroupChat;
	}

	public void setGroupChat(boolean groupChat) {
		this.isGroupChat = groupChat;
	}
}