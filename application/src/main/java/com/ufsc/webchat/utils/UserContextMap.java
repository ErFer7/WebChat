package com.ufsc.webchat.utils;

import java.util.HashMap;

import org.javatuples.Pair;

public class UserContextMap {

	// userId -> (token, clientId)
	private final HashMap<Long, Pair<String, String>> users;

	public UserContextMap() {
		this.users = new HashMap<>();
	}

	public void add(Long userId, String token) {
		this.users.put(userId, new Pair<>(token, null));
	}

	public void remove(Long userId) {
		this.users.remove(userId);
	}

	public void setUserClientId(Long userId, String clientId) {
		Pair<String, String> tokenClientIdPair = this.users.get(userId).setAt1(clientId);
		this.users.put(userId, tokenClientIdPair);
	}

	public String getToken(Long userId) {
		return this.users.get(userId).getValue0();
	}

	public String getClientId(Long userId) {
		return this.users.get(userId).getValue1();
	}

}
