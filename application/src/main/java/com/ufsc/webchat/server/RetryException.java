package com.ufsc.webchat.server;

//import static com.ufsc.webchat.server.ClientPacketProcessor.logger;

public class RetryException extends Exception {
    public RetryException() {
        super();
    }

    public RetryException(String message) {
        super(message);
    }

    public void logError(String message) {
//        logger.error("Invalid payload"); //?
    }
}
