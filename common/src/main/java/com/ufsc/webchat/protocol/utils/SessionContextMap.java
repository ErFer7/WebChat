package com.ufsc.webchat.protocol.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snf4j.websocket.IWebSocketSession;

public class SessionContextMap {

	private final HashMap<Long, Integer> sessionIds;
	private final HashMap<String, Integer> hosts;
	private final HashMap<String, Integer> processIds;
	private final HashMap<Integer, IWebSocketSession> sessions;
	private int index;

	public SessionContextMap() {
		this.sessionIds = new HashMap<>();
		this.hosts = new HashMap<>();
		this.processIds = new HashMap<>();
		this.sessions = new HashMap<>();
		this.index = 0;
	}

	public void addSession(Long sessionId, String host, IWebSocketSession session) {

		this.sessionIds.put(sessionId, this.index);
		this.hosts.put(host, this.index);

		this.sessions.put(this.index, session);

		this.index++;
	}

	public void associateToId(String host, String id){
		this.processIds.put(id, this.hosts.get(host));
	}

	public String getProcessIdBySessionId(Long sessionId){
		Integer targetIndex = this.sessionIds.get(sessionId);

		for(Map.Entry<String, Integer> entry : this.processIds.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public IWebSocketSession getById(String id) {
		return this.sessions.get(this.processIds.get(id));
	}

	public List<IWebSocketSession> getClosedSessions() {
		List<IWebSocketSession> closedSessions = new ArrayList<>();

		for(Map.Entry<Integer, IWebSocketSession> entry : this.sessions.entrySet()) {
			if (!entry.getValue().isOpen()) {
				closedSessions.add(entry.getValue());
			}
		}

		return closedSessions;
	}

	public void removeBySessionId(Long sessionId) {
		Integer targetIndex = this.sessionIds.get(sessionId);

		this.sessions.remove(targetIndex);
		this.sessionIds.remove(sessionId);

		for(Map.Entry<String, Integer> entry : this.hosts.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				this.hosts.remove(entry.getKey());
				break;
			}
		}

		for(Map.Entry<String, Integer> entry : this.processIds.entrySet()) {
			if (entry.getValue().equals(targetIndex)) {
				this.processIds.remove(entry.getKey());
				break;
			}
		}
	}
}
