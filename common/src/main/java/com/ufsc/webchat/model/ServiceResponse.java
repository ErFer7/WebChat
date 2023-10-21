package com.ufsc.webchat.model;

import com.ufsc.webchat.protocol.enums.Status;

public record ServiceResponse(Status status, String message, Object payload) {
}
