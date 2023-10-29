package com.ufsc.webchat.utils.retry;

import java.io.Serial;

public class RetryException extends Exception {
	@Serial private static final long serialVersionUID = -2912419945463621961L;

	public RetryException() {
		super();
	}

	public RetryException(String message) {
		super(message);
	}

}
