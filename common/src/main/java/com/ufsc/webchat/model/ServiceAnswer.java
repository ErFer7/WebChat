package com.ufsc.webchat.model;

import com.ufsc.webchat.protocol.enums.Status;

public record ServiceAnswer(Status status, String message) {
}
