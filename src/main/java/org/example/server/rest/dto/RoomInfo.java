package org.example.server.rest.dto;

public class RoomInfo {
    private final String name;
    private final int userCount;

    public RoomInfo(String name, int userCount) {
        this.name = name;
        this.userCount = userCount;
    }

    public String getName() {
        return name;
    }

    public int getUserCount() {
        return userCount;
    }
}
