package org.example.server.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private static final int MAX_HISTORY_SIZE = 20;

    private final String name;
    private final CopyOnWriteArrayList<User> users;
    private final CopyOnWriteArrayList<ChatMessage> messageHistory;
    private final LocalDateTime createdAt;

    public Room(String name) {
        this.name = name;
        this.users = new CopyOnWriteArrayList<>();
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public synchronized void addUser(User user) {
        if (!users.contains(user)) {
            users.add(user);
        }
    }

    public synchronized void removeUser(User user) {
        users.remove(user);
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public int getUserCount() {
        return users.size();
    }

    public String getName() {
        return name;
    }

    public synchronized void addMessage(ChatMessage message) {
        messageHistory.add(message);

        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.remove(0);
        }
    }

    public List<ChatMessage> getHistory(int limit) {
        List<ChatMessage> history = new ArrayList<>(messageHistory);
        int size = history.size();

        if (size <= limit) {
            return history;
        }

        return history.subList(size - limit, size);
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", userCount=" + users.size() +
                '}';
    }
}
