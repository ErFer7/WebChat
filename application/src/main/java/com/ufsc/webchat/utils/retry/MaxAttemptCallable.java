package com.ufsc.webchat.utils.retry;

@FunctionalInterface
public interface MaxAttemptCallable {
	void call(String message);
}
