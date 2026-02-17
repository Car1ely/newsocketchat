package org.example.server.websocket;

import org.example.protocol.MessageSender;
import jakarta.websocket.Session;

import java.io.IOException;

public class WsMessageSender implements MessageSender {
    private final Session session;

    public WsMessageSender(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        this.session = session;
    }

    @Override
    public void sendMessage(String message) {
        if (isConnected()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @Override
    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                System.err.println("Error closing WebSocket session: " + e.getMessage());
            }
        }
    }
}
