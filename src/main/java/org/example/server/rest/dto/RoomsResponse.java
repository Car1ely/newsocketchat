package org.example.server.rest.dto;

import java.util.List;

public class RoomsResponse {
    private final boolean success;
    private final List<RoomInfo> rooms;

    public RoomsResponse(boolean success, List<RoomInfo> rooms) {
        this.success = success;
        this.rooms = rooms;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<RoomInfo> getRooms() {
        return rooms;
    }
}
