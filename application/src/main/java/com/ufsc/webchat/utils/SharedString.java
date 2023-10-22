package com.ufsc.webchat.utils;

public class SharedString {
	private String sharedValue;

	public SharedString() {
		this.sharedValue = "";
	}

	public String getString() {
		return this.sharedValue;
	}

	public void setString(String value) {
		this.sharedValue = value;
	}
}
