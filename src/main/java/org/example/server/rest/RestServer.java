package org.example.server.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.Protocol;
import org.example.server.RoomManager;
import org.example.server.model.ChatMessage;
import org.example.server.model.User;
import org.example.server.rest.dto.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RestServer {
    private final int port;
    private final RoomManager roomManager;
    private final SessionManager sessionManager;
    private final Gson gson;
    private Javalin app;

    public RestServer(int port) {
        this.port = port;
        this.roomManager = new RoomManager();
        this.sessionManager = new SessionManager(roomManager);
        this.gson = new Gson();
    }

    public void start() {
        app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
            config.jsonMapper(new io.javalin.json.JsonMapper() {
                @Override
                public String toJsonString(Object obj, java.lang.reflect.Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public <T> T fromJsonString(String json, java.lang.reflect.Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(port);

        System.out.println("REST API Server started on http://localhost:" + port + Protocol.REST_BASE_PATH);
        System.out.println("Endpoints:");
        System.out.println("  POST " + Protocol.REST_BASE_PATH + "/join");
        System.out.println("  POST " + Protocol.REST_BASE_PATH + "/send");
        System.out.println("  GET  " + Protocol.REST_BASE_PATH + "/messages");
        System.out.println("  GET  " + Protocol.REST_BASE_PATH + "/rooms");
        System.out.println("  GET  " + Protocol.REST_BASE_PATH + "/users");
        System.out.println("  GET  " + Protocol.REST_BASE_PATH + "/history");
        System.out.println("Press Ctrl+C to stop...");

        app.post(Protocol.REST_BASE_PATH + "/join", this::handleJoin);
        app.post(Protocol.REST_BASE_PATH + "/send", this::handleSend);
        app.get(Protocol.REST_BASE_PATH + "/messages", this::handleMessages);
        app.get(Protocol.REST_BASE_PATH + "/rooms", this::handleRooms);
        app.get(Protocol.REST_BASE_PATH + "/users", this::handleUsers);
        app.get(Protocol.REST_BASE_PATH + "/history", this::handleHistory);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void handleJoin(Context ctx) {
        JoinRequest request = ctx.bodyAsClass(JoinRequest.class);

        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            ctx.json(new JoinResponse(false, null, "Nickname cannot be empty"));
            return;
        }
        if (request.getRoom() == null || request.getRoom().trim().isEmpty()) {
            ctx.json(new JoinResponse(false, null, "Room name cannot be empty"));
            return;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(ctx.ip());
        } catch (Exception e) {
            address = null;
        }

        RestMessageSender messageSender = new RestMessageSender();
        User user = new User(request.getNickname(), messageSender, address);

        if (!roomManager.registerUser(user)) {
            ctx.json(new JoinResponse(false, null, "Nickname already in use"));
            return;
        }

        if (!roomManager.addUserToRoom(user, request.getRoom())) {
            roomManager.unregisterUser(user);
            ctx.json(new JoinResponse(false, null, "Failed to join room"));
            return;
        }

        String sessionId = sessionManager.createSession(user);

        ctx.json(new JoinResponse(true, sessionId, "Joined room: " + request.getRoom()));
    }

    private void handleSend(Context ctx) {
        SendRequest request = ctx.bodyAsClass(SendRequest.class);

        User user = sessionManager.getUser(request.getSessionId());
        if (user == null) {
            ctx.json(new SendResponse(false, "Invalid or expired session"));
            return;
        }

        if (user.getCurrentRoom() == null) {
            ctx.json(new SendResponse(false, "Not in any room"));
            return;
        }

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            ctx.json(new SendResponse(false, "Message cannot be empty"));
            return;
        }

        roomManager.broadcastAndSave(user.getCurrentRoom(), MessageType.BROADCAST,
                user.getNickname(), request.getText(), user);

        ctx.json(new SendResponse(true, "Message sent"));
    }

    private void handleMessages(Context ctx) {
        String sessionId = ctx.queryParam("sessionId");

        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            ctx.json(new MessagesResponse(false, new ArrayList<>()));
            return;
        }

        if (!(user.getMessageSender() instanceof RestMessageSender)) {
            ctx.json(new MessagesResponse(false, new ArrayList<>()));
            return;
        }

        RestMessageSender restSender = (RestMessageSender) user.getMessageSender();

        List<String> rawMessages;
        try {
            rawMessages = restSender.pollMessages(Protocol.REST_LONG_POLL_TIMEOUT_MS);
        } catch (InterruptedException e) {
            ctx.json(new MessagesResponse(false, new ArrayList<>()));
            return;
        }

        List<JsonObject> messages = new ArrayList<>();
        for (String raw : rawMessages) {
            try {
                JsonObject msgObj = gson.fromJson(raw, JsonObject.class);
                messages.add(msgObj);
            } catch (Exception e) {
                System.err.println("Error parsing message JSON: " + e.getMessage());
            }
        }

        ctx.json(new MessagesResponse(true, messages));
    }

    private void handleRooms(Context ctx) {
        String sessionId = ctx.queryParam("sessionId");

        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            ctx.status(401).result("Invalid session");
            return;
        }

        List<RoomManager.RoomInfo> roomInfos = roomManager.getAllRooms();
        List<org.example.server.rest.dto.RoomInfo> dtoRooms = roomInfos.stream()
                .map(r -> new org.example.server.rest.dto.RoomInfo(r.getName(), r.getUserCount()))
                .collect(Collectors.toList());

        ctx.json(new RoomsResponse(true, dtoRooms));
    }

    private void handleUsers(Context ctx) {
        String sessionId = ctx.queryParam("sessionId");

        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            ctx.status(401).result("Invalid session");
            return;
        }

        List<String> users = roomManager.getAllActiveUsers();
        ctx.json(new UsersResponse(true, users));
    }

    private void handleHistory(Context ctx) {
        String sessionId = ctx.queryParam("sessionId");

        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            ctx.status(401).result("Invalid session");
            return;
        }

        if (user.getCurrentRoom() == null) {
            ctx.json(new HistoryResponse(false, new ArrayList<>(), "Not in any room"));
            return;
        }

        List<ChatMessage> history = roomManager.getRoomHistory(user.getCurrentRoom(), 20);
        List<String> formattedHistory = history.stream()
                .map(ChatMessage::format)
                .collect(Collectors.toList());

        ctx.json(new HistoryResponse(true, formattedHistory, null));
    }

    public void shutdown() {
        System.out.println("\nShutting down REST API server...");
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        if (app != null) {
            app.stop();
        }
        System.out.println("REST API server shutdown complete");
    }

    public static void main(String[] args) {
        int port = Protocol.DEFAULT_REST_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Using default: " + port);
            }
        }

        RestServer server = new RestServer(port);
        server.start();
    }
}
