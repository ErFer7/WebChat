package com.ufsc.webchat.server;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHandler {

	private PasswordHandler() {
	}

	public static String generatePasswordHash(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

	public static Boolean validatePasswordWithHash(String password, String hash) {
		return BCrypt.checkpw(password, hash);
	}
}