package org.example.server.rest.dto;

public class SendResponse {
    private boolean success;
    private String message;

    public SendResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

}
