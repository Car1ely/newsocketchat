package org.example.server.rest;

import org.example.protocol.MessageSender;
import org.example.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RestMessageSender implements MessageSender {
    private final BlockingQueue<String> messageQueue;
    private volatile boolean connected = true;

    public RestMessageSender() {
        this.messageQueue = new LinkedBlockingQueue<>(Protocol.REST_MESSAGE_QUEUE_SIZE);
    }

    @Override
    public void sendMessage(String message) {
        if (connected && message != null) {
            try {
                if (!messageQueue.offer(message)) {
                    System.err.println("REST message queue full, dropping message");
                }
            } catch (Exception e) {
                System.err.println("Error queuing REST message: " + e.getMessage());
            }
        }
    }

    public List<String> pollMessages(long timeoutMs) throws InterruptedException {
        List<String> messages = new ArrayList<>();

        String first = messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (first != null) {
            messages.add(first);
            messageQueue.drainTo(messages);
        }

        return messages;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        connected = false;
        messageQueue.clear();
    }

}
