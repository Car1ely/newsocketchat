package org.example.server.rest.dto;

import java.util.List;

public class UsersResponse {
    private final boolean success;
    private final List<String> users;

    public UsersResponse(boolean success, List<String> users) {
        this.success = success;
        this.users = users;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getUsers() {
        return users;
    }
}
