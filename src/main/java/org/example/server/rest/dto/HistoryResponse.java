package org.example.server.rest.dto;

import java.util.List;

public class HistoryResponse {
    private final boolean success;
    private final List<String> history;
    private final String error;

    public HistoryResponse(boolean success, List<String> history, String error) {
        this.success = success;
        this.history = history;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getHistory() {
        return history;
    }

    public String getError() {
        return error;
    }
}
