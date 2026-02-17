package org.example.server.rest.dto;

public class JoinResponse {
    private boolean success;
    private String sessionId;
    private String message;

    public JoinResponse(boolean success, String sessionId, String message) {
        this.success = success;
        this.sessionId = sessionId;
        this.message = message;
    }

}
