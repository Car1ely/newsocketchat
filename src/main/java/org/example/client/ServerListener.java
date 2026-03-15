package org.example.client;

import org.example.protocol.Message;
import org.example.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    private final BufferedReader in;
    private final ChatClient client;

    public ServerListener(BufferedReader in, ChatClient client) {
        this.in = in;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            String line;
            while (client.isRunning() && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Message message = Message.parse(line);
                    handleMessage(message);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid message from server: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (client.isRunning()) {
                System.err.println("Connection to server lost: " + e.getMessage());
            }
        } finally {
            client.shutdown();
        }
    }

    private void handleMessage(Message message) {
        MessageType type = message.getType();

        switch (type) {
            case BROADCAST:
                handleBroadcast(message);
                break;
            case USER_JOINED:
                handleUserJoined(message);
                break;
            case USER_LEFT:
                handleUserLeft(message);
                break;
            case JOIN_ACK:
                handleJoinAck(message);
                break;
            case ROOM_LIST:
                displayRoomList(message);
                break;
            case USER_LIST:
                displayUserList(message);
                break;
            case HISTORY_LIST:
                displayHistory(message);
                break;
            case ERROR:
                handleError(message);
                break;
            default:
                System.out.println("Received: " + message.serialize());
        }
    }

    private void handleBroadcast(Message message) {
        String sender = message.getParam(0);
        String text = message.getParam(1);
        System.out.println("[" + sender + "]: " + text);
    }

    private void handleUserJoined(Message message) {
        String nickname = message.getParam(0);
        System.out.println("* " + nickname + " joined the room");
    }

    private void handleUserLeft(Message message) {
        String nickname = message.getParam(0);
        System.out.println("* " + nickname + " left the room");
    }

    private void handleJoinAck(Message message) {
        String roomName = message.getParam(0);
        boolean success = Boolean.parseBoolean(message.getParam(1));

        if (success) {
            System.out.println("Joined room: " + roomName);
        } else {
            System.err.println("Failed to join room: " + roomName);
        }
    }

    private void displayRoomList(Message message) {
        System.out.println("\n=== Active Rooms ===");

        String[] params = message.getParams();
        if (params.length == 0) {
            System.out.println("  No active rooms");
        } else {
            for (String roomData : params) {
                String[] parts = roomData.split(":");
                String roomName = parts[0];
                String userCount = parts.length > 1 ? parts[1] : "0";
                System.out.println("  " + roomName + " (" + userCount + " users)");
            }
        }

        System.out.println("====================\n");
    }

    private void displayUserList(Message message) {
        System.out.println("\n=== Active Users ===");

        String[] params = message.getParams();
        if (params.length == 0) {
            System.out.println("  No active users");
        } else {
            for (String user : params) {
                System.out.println("  - " + user);
            }
        }

        System.out.println("====================\n");
    }

    private void displayHistory(Message message) {
        System.out.println("\n=== Message History ===");

        String[] params = message.getParams();
        if (params.length == 0) {
            System.out.println("  No messages yet");
        } else {
            for (String formattedMsg : params) {
                System.out.println(formattedMsg);
            }
        }

        System.out.println("=======================\n");
    }

    private void handleError(Message message) {
        String errorMsg = message.getParam(0);
        System.err.println("Error: " + errorMsg);
    }
}
