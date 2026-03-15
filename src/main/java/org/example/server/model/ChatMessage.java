package org.example.server.model;

import org.example.protocol.MessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private final MessageType type;
    private final String sender;
    private final String text;
    private final LocalDateTime timestamp;

    public ChatMessage(MessageType type, String sender, String text) {
        this.type = type;
        this.sender = sender;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String format() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = timestamp.format(formatter);

        switch (type) {
            case BROADCAST:
                return String.format("[%s] %s: %s", time, sender, text);
            case USER_JOINED:
                return String.format("[%s] * %s joined the room", time, sender);
            case USER_LEFT:
                return String.format("[%s] * %s left the room", time, sender);
            default:
                return String.format("[%s] %s", time, text);
        }
    }
}
