package com.ufsc.webchat.database.service;

import com.ufsc.webchat.protocol.enums.Status;

public record Answer(Status status, String message) {
}
