package com.ufsc.webchat.utils;

import java.util.HashMap;

import org.javatuples.Pair;

public class UserContextMap {

	private final HashMap<Long, Pair<String, String>> userIdTokenHostMap;

	public UserContextMap() {
		this.userIdTokenHostMap = new HashMap<>();
	}

	public void add(Long userId, String token) {
		this.userIdTokenHostMap.put(userId, new Pair<>(token, null));
	}

	public void remove(Long userId) {
		this.userIdTokenHostMap.remove(userId);
	}

	public void setUserHost(Long userId, String host) {
		Pair<String, String> tokenHostPair = this.userIdTokenHostMap.get(userId).setAt1(host);
		this.userIdTokenHostMap.put(userId, tokenHostPair);
	}

	public String getUserToken(Long userId) {
		return this.userIdTokenHostMap.get(userId).getValue0();
	}

	public String getUserHost(Long userId) {
		return this.userIdTokenHostMap.get(userId).getValue1();
	}

}
