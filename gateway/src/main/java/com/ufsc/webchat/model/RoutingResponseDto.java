package com.ufsc.webchat.model;

import com.ufsc.webchat.protocol.enums.Status;

public class RoutingResponseDto {

	private final Status status;
	private String token;
	private Long userId;
	private String applicationHost;

	public RoutingResponseDto(Status status, String token, Long userId, String applicationHost) {
		this.status = status;
		this.token = token;
		this.userId = userId;
		this.applicationHost = applicationHost;
	}

	public RoutingResponseDto(Status status) {
		this.status = status;
	}

	public Status getStatus() {
		return this.status;
	}

	public String getToken() {
		return this.token;
	}

	public Long getUserId() {
		return this.userId;
	}

	public String getApplicationHost() {
		return this.applicationHost;
	}

}
