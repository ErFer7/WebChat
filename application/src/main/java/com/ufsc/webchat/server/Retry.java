package com.ufsc.webchat.server;

import com.ufsc.webchat.protocol.Packet;

import java.util.Timer;
import java.util.TimerTask;

public class Retry {
    public static void launch(Packet packet, ClientPacketProcessor clientPacketProcessor) {
        Timer timer = new Timer();

        TimerTask task = new RetryTask(packet, clientPacketProcessor);

        // Schedule the task to run after 2000 milliseconds (2 seconds)
        timer.schedule(task, 2000);
    }

    protected static class RetryTask extends TimerTask {
        private final Packet packet;
        private final ClientPacketProcessor clientPacketProcessor;

        public RetryTask(Packet packet, ClientPacketProcessor clientPacketProcessor) {
            this.packet = packet;
            this.clientPacketProcessor = clientPacketProcessor;
        }

        @Override
        public void run() {
            this.clientPacketProcessor.tryAgain(this.packet);
        }
    }

}