package org.example.server.rest.dto;

import com.google.gson.JsonObject;
import java.util.List;

public class MessagesResponse {
    private boolean success;
    private List<JsonObject> messages;

    public MessagesResponse(boolean success, List<JsonObject> messages) {
        this.success = success;
        this.messages = messages;
    }

}
