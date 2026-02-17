package org.example.server.model;

import org.example.protocol.MessageSender;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private final String nickname;
    private String currentRoom;
    private final MessageSender messageSender;
    private final LocalDateTime connectedAt;
    private final InetAddress address;

    public User(String nickname, MessageSender messageSender, InetAddress address) {
        this.nickname = nickname;
        this.messageSender = messageSender;
        this.address = address;
        this.connectedAt = LocalDateTime.now();
        this.currentRoom = null;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public void sendMessage(String message) {
        if (messageSender != null && messageSender.isConnected()) {
            messageSender.sendMessage(message);
        }
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(nickname, user.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname);
    }

    @Override
    public String toString() {
        return "User{" +
                "nickname='" + nickname + '\'' +
                ", currentRoom='" + currentRoom + '\'' +
                ", address=" + address +
                '}';
    }
}
