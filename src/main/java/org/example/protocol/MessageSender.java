package org.example.protocol;

public interface MessageSender {
    void sendMessage(String message);
    boolean isConnected();
    void close();
}
