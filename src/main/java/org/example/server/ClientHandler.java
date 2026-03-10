package org.example.server;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.server.model.User;
import org.example.server.tcp.TcpMessageSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RoomManager roomManager;
    private BufferedReader in;
    private PrintWriter out;
    private User user;
    private volatile boolean running;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("New client connected from " + socket.getInetAddress());

            String line;
            while (running && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Message message = Message.parse(line);
                    handleMessage(message);
                } catch (IllegalArgumentException e) {
                    sendError("Invalid message format: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(Message message) {
        MessageType type = message.getType();

        switch (type) {
            case CONNECT:
                handleConnect(message.getParam(0));
                break;
            case JOIN:
                handleJoin(message.getParam(0));
                break;
            case MESSAGE:
                handleChatMessage(message.getParam(0));
                break;
            case DISCONNECT:
                running = false;
                break;
            default:
                sendError("Unsupported message type: " + type);
        }
    }

    private void handleConnect(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            sendError("Nickname cannot be empty");
            Message ack = Message.createConnectAck(false, "Nickname cannot be empty");
            out.println(ack.serialize());
            return;
        }

        user = new User(nickname.trim(), new TcpMessageSender(out), socket.getInetAddress());

        if (!roomManager.registerUser(user)) {
            sendError("Nickname already taken");
            Message ack = Message.createConnectAck(false, "Nickname already taken");
            out.println(ack.serialize());
            user = null;
            return;
        }

        Message ack = Message.createConnectAck(true, "Welcome " + nickname + "!");
        out.println(ack.serialize());

        System.out.println("User connected: " + nickname + " from " + socket.getInetAddress());
    }

    private void handleJoin(String roomName) {
        if (user == null) {
            sendError("Not connected. Send CONNECT first.");
            return;
        }

        if (roomName == null || roomName.trim().isEmpty()) {
            sendError("Room name cannot be empty");
            Message ack = Message.createJoinAck("", false);
            out.println(ack.serialize());
            return;
        }

        boolean success = roomManager.addUserToRoom(user, roomName.trim());

        if (success) {
            Message ack = Message.createJoinAck(roomName.trim(), true);
            out.println(ack.serialize());
            System.out.println("User " + user.getNickname() + " joined room: " + roomName);
        } else {
            Message ack = Message.createJoinAck(roomName.trim(), false);
            out.println(ack.serialize());
            sendError("Failed to join room");
        }
    }

    private void handleChatMessage(String text) {
        if (user == null) {
            sendError("Not connected. Send CONNECT first.");
            return;
        }

        if (user.getCurrentRoom() == null) {
            sendError("Not in a room. Join a room first.");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            sendError("Message cannot be empty");
            return;
        }

        Message broadcast = Message.createBroadcast(user.getNickname(), text);
        roomManager.broadcastToRoom(user.getCurrentRoom(), broadcast.serialize(), null);
    }

    private void sendError(String errorMessage) {
        Message error = Message.createError(errorMessage);
        out.println(error.serialize());
    }

    private void disconnect() {
        if (user != null) {
            System.out.println("User disconnected: " + user.getNickname());
            roomManager.removeUserFromRoom(user);
            roomManager.unregisterUser(user);
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        System.out.println("Client connection closed");
    }
}
