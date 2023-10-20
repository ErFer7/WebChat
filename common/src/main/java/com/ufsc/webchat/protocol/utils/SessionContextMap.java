package com.ufsc.webchat.protocol.utils;

import java.util.HashMap;
import java.util.Map;

import org.snf4j.websocket.IWebSocketSession;

public class SessionContextMap {

	private final HashMap<String, Integer> names;
	private final HashMap<String, Integer> hosts;
	private final HashMap<String, Integer> ids;
	private final HashMap<Integer, IWebSocketSession> sessions;
	private int index;

	public SessionContextMap() {
		this.names = new HashMap<>();
		this.hosts = new HashMap<>();
		this.ids = new HashMap<>();
		this.sessions = new HashMap<>();
		this.index = 0;
	}

	public void addSession(String name, String host, IWebSocketSession session) {

		this.names.put(name, this.index);
		this.hosts.put(host, this.index);

		this.sessions.put(this.index, session);

		this.index++;
	}

	public void associateToId(String host, String id){
		this.ids.put(id, this.hosts.get(host));
	}

	public String getIdByName(String name){
		Integer targetIndex = this.names.get(name);

		for(Map.Entry<String, Integer> entry : this.ids.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public String getHostById(String id){
		Integer targetIndex = this.ids.get(id);

		for(Map.Entry<String, Integer> entry : this.hosts.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public IWebSocketSession getById(String id) {
		return this.sessions.get(this.ids.get(id));
	}

	public IWebSocketSession getByHost(String host) {
		return this.sessions.get(this.hosts.get(host));
	}

	public void removeByName(String name) {
		this.remove(name, this.names, this.hosts, this.ids);
	}

	public void removeByHost(String host) {
		this.remove(host, this.hosts, this.names, this.ids);
	}

	public void removeById(String id) {
		this.remove(id, this.ids, this.names, this.hosts);
	}

	private void remove(String key, HashMap<String, Integer> firstMap, HashMap<String, Integer> secondMap, HashMap<String, Integer> thirdMap) {
		Integer targetIndex = firstMap.get(key);

		this.sessions.remove(targetIndex);
		firstMap.remove(key);

		for(Map.Entry<String, Integer> entry : secondMap.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				secondMap.remove(entry.getKey());
				break;
			}
		}

		for(Map.Entry<String, Integer> entry : thirdMap.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				thirdMap.remove(entry.getKey());
				break;
			}
		}
	}
}
