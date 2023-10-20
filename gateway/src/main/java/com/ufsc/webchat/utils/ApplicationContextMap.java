package com.ufsc.webchat.utils;

import java.util.HashMap;
import java.util.Map;

import org.javatuples.Triplet;

public class ApplicationContextMap {

	// ID, Token, External Host, User Count
	private final HashMap<String, Triplet<String, String, Integer>> applications;

	public ApplicationContextMap() {
		this.applications = new HashMap<>();
	}

	public void add(String id, String token, String externalHost) {
		this.applications.put(id, new Triplet<>(token, externalHost, 0));
	}

	public void remove(String id) {
		this.applications.remove(id);
	}

	public void incrementUserCount(String host) {
		Triplet<String, String, Integer> triplet = this.applications.get(host);
		this.applications.put(host, triplet.setAt2(triplet.getValue2() + 1));
	}

	public void decrementUserCount(String host) {
		Triplet<String, String, Integer> triplet = this.applications.get(host);
		this.applications.put(host, triplet.setAt2(triplet.getValue2() - 1));
	}

	public String getToken(String host) {
		return this.applications.get(host).getValue0();
	}

	public String getExternalHost(String id) {
		return this.applications.get(id).getValue1();
	}

	public Integer getUserCount(String host) {
		return this.applications.get(host).getValue2();
	}

	public String chooseLeastLoadedApplication() {
		String id = null;
		int smallestUserCount = Integer.MAX_VALUE;

		for (Map.Entry<String, Triplet<String, String, Integer>> entry : this.applications.entrySet()) {
			if (entry.getValue().getValue2() < smallestUserCount) {
				id = entry.getKey();
				smallestUserCount = entry.getValue().getValue2();
			}
		}

		return id;
	}
}
