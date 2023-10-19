package com.ufsc.webchat.utils;

import java.util.HashMap;
import java.util.Map;

import org.javatuples.Pair;

public class ApplicationContextMap {

	private final HashMap<String, Pair<String, Integer>> applicationHostTokenUserCountMap;

	public ApplicationContextMap() {
		this.applicationHostTokenUserCountMap = new HashMap<>();
	}

	public void add(String host, String token) {
		this.applicationHostTokenUserCountMap.put(host, new Pair<>(token, 0));
	}

	public void remove(String host) {
		this.applicationHostTokenUserCountMap.remove(host);
	}

	public void incrementUserCount(String host) {
		Pair<String, Integer> pair = this.applicationHostTokenUserCountMap.get(host);
		this.applicationHostTokenUserCountMap.put(host, new Pair<>(pair.getValue0(), pair.getValue1() + 1));
	}

	public void decrementUserCount(String host) {
		Pair<String, Integer> pair = this.applicationHostTokenUserCountMap.get(host);
		this.applicationHostTokenUserCountMap.put(host, new Pair<>(pair.getValue0(), pair.getValue1() - 1));
	}

	public String getToken(String host) {
		return this.applicationHostTokenUserCountMap.get(host).getValue0();
	}

	public Integer getUserCount(String host) {
		return this.applicationHostTokenUserCountMap.get(host).getValue1();
	}

	public String chooseLeastLoadedApplication() {
		String host = null;
		int smallestUserCount = Integer.MAX_VALUE;

		for (Map.Entry<String, Pair<String, Integer>> entry : this.applicationHostTokenUserCountMap.entrySet()) {
			if (entry.getValue().getValue1() < smallestUserCount) {
				host = entry.getKey();
				smallestUserCount = entry.getValue().getValue1();
			}
		}

		return host;
	}
}
