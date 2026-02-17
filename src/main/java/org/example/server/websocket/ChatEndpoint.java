package org.example.server.websocket;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.Protocol;
import org.example.server.RoomManager;
import org.example.server.model.User;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat")
public class ChatEndpoint {

    private static RoomManager roomManager;
    private static final Map<String, User> sessionUsers = new ConcurrentHashMap<>();

    private Session session;
    private User user;

    public static void setRoomManager(RoomManager manager) {
        roomManager = manager;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        session.setMaxIdleTimeout(Protocol.WS_IDLE_TIMEOUT);
        session.setMaxTextMessageBufferSize(Protocol.WS_MAX_BUFFER_SIZE);

        System.out.println("WebSocket connection opened: " + session.getId());
    }

    @OnMessage
    public void onMessage(String messageJson, Session session) {
        try {
            Message message = Message.fromJson(messageJson);
            handleMessage(message);

        } catch (IllegalArgumentException e) {
            sendError("Invalid message format: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket connection closed: " + session.getId()
                + " (" + reason.getReasonPhrase() + ")");
        disconnect();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error on session " + session.getId()
                + ": " + error.getMessage());
        disconnect();
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
                disconnect();
                break;
            default:
                sendError("Unsupported message type: " + type);
        }
    }

    private void handleConnect(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            Message ack = Message.createConnectAck(false, "Nickname cannot be empty");
            sendJson(ack);
            return;
        }

        WsMessageSender sender = new WsMessageSender(session);
        InetAddress address = getClientAddress();
        user = new User(nickname.trim(), sender, address);

        if (!roomManager.registerUser(user)) {
            Message ack = Message.createConnectAck(false, "Nickname already taken");
            sendJson(ack);
            user = null;
            return;
        }

        sessionUsers.put(session.getId(), user);

        Message ack = Message.createConnectAck(true, "Welcome " + nickname + "!");
        sendJson(ack);

        System.out.println("WebSocket user connected: " + nickname);
    }

    private void handleJoin(String roomName) {
        if (user == null) {
            sendError("Not connected. Send CONNECT first.");
            return;
        }

        if (roomName == null || roomName.trim().isEmpty()) {
            Message ack = Message.createJoinAck("", false);
            sendJson(ack);
            return;
        }

        boolean success = roomManager.addUserToRoom(user, roomName.trim());
        Message ack = Message.createJoinAck(roomName.trim(), success);
        sendJson(ack);

        System.out.println("User " + user.getNickname() + " joined room: " + roomName);
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
        System.out.println("[" + user.getCurrentRoom() + "] " + user.getNickname() + ": " + text);
        roomManager.broadcastToRoom(user.getCurrentRoom(), broadcast.toJson(), null);
    }

    private void sendJson(Message message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message.toJson());
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    private void sendError(String errorMessage) {
        Message error = Message.createError(errorMessage);
        sendJson(error);
    }

    private void disconnect() {
        if (user != null) {
            System.out.println("WebSocket user disconnected: " + user.getNickname());
            roomManager.removeUserFromRoom(user);
            roomManager.unregisterUser(user);
            sessionUsers.remove(session.getId());
            user = null;
        }
    }

    private InetAddress getClientAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return null;
        }
    }
}
