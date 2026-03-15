package org.example.server;

import org.example.protocol.Message;
import org.example.server.model.Room;
import org.example.server.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final ConcurrentHashMap<String, Room> rooms;
    private final ConcurrentHashMap<String, User> activeUsers;

    public static class RoomInfo {
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

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
        this.activeUsers = new ConcurrentHashMap<>();
    }

    public synchronized boolean registerUser(User user) {
        if (activeUsers.containsKey(user.getNickname())) {
            return false;
        }
        activeUsers.put(user.getNickname(), user);
        return true;
    }

    public synchronized void unregisterUser(User user) {
        activeUsers.remove(user.getNickname());
    }

    public synchronized boolean addUserToRoom(User user, String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return false;
        }

        if (user.getCurrentRoom() != null) {
            removeUserFromRoom(user);
        }

        Room room = rooms.computeIfAbsent(roomName, Room::new);

        room.addUser(user);
        user.setCurrentRoom(roomName);

        Message joinMsg = Message.createUserJoined(user.getNickname());
        broadcastToRoom(roomName, joinMsg.serialize(), user);

        return true;
    }

    public synchronized void removeUserFromRoom(User user) {
        String currentRoom = user.getCurrentRoom();
        if (currentRoom == null) {
            return;
        }

        Room room = rooms.get(currentRoom);
        if (room != null) {
            room.removeUser(user);

            Message leftMsg = Message.createUserLeft(user.getNickname());
            broadcastToRoom(currentRoom, leftMsg.serialize(), null);

            if (room.getUserCount() == 0) {
                rooms.remove(currentRoom);
            }
        }

        user.setCurrentRoom(null);
    }

    public void broadcastToRoom(String roomName, String message, User sender) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return;
        }

        List<User> users = room.getUsers();

        for (User user : users) {
            if (sender == null || !user.equals(sender)) {
                user.sendMessage(message);
            }
        }
    }

    public List<RoomInfo> getAllRooms() {
        List<RoomInfo> roomList = new ArrayList<>();
        for (Room room : rooms.values()) {
            roomList.add(new RoomInfo(room.getName(), room.getUserCount()));
        }
        return roomList;
    }

    public List<String> getAllActiveUsers() {
        return new ArrayList<>(activeUsers.keySet());
    }

}
