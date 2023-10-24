package com.ufsc.webchat.utils;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;

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

	public Long getUserIdByClientId(String clientId) {
		return this.users.entrySet().stream()
				.filter(entry -> entry.getValue().getValue1().equals(clientId))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public String getToken(Long userId) {
		Pair<String, String> tokenClientIdPair = this.users.get(userId);

		if (isNull(tokenClientIdPair)) {
			return null;
		}

		return tokenClientIdPair.getValue0();
	}

	public String getClientId(Long userId) {
		Pair<String, String> tokenClientIdPair = this.users.get(userId);

		if (isNull(tokenClientIdPair)) {
			return null;
		}

		return tokenClientIdPair.getValue1();
	}

}
