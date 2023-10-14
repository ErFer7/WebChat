package com.ufsc.webchat.utils;

import java.time.Instant;

public class Logger {

	public static void log(String className, String message) {
		System.out.println("[" + Instant.now() + "]" + "[" + className + "]: " + message);
	}
}
