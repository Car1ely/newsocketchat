package org.example.server.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.protocol.Message;
import org.example.protocol.Protocol;
import org.example.server.RoomManager;
import org.example.server.model.User;
import org.example.server.rest.dto.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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
        System.out.println("Press Ctrl+C to stop...");

        app.post(Protocol.REST_BASE_PATH + "/join", this::handleJoin);
        app.post(Protocol.REST_BASE_PATH + "/send", this::handleSend);
        app.get(Protocol.REST_BASE_PATH + "/messages", this::handleMessages);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void handleJoin(Context ctx) {
        try {
            JoinRequest request = ctx.bodyAsClass(JoinRequest.class);

            if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
                ctx.json(new JoinResponse(false, null, "Nickname cannot be empty"));
                return;
            }
            if (request.getRoom() == null || request.getRoom().trim().isEmpty()) {
                ctx.json(new JoinResponse(false, null, "Room name cannot be empty"));
                return;
            }

            RestMessageSender messageSender = new RestMessageSender();
            InetAddress address = InetAddress.getByName(ctx.ip());
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

        } catch (Exception e) {
            ctx.json(new JoinResponse(false, null, "Error: " + e.getMessage()));
        }
    }

    private void handleSend(Context ctx) {
        try {
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

            Message broadcast = Message.createBroadcast(user.getNickname(), request.getText());
            roomManager.broadcastToRoom(user.getCurrentRoom(), broadcast.toJson(), user);

            ctx.json(new SendResponse(true, "Message sent"));

        } catch (Exception e) {
            ctx.json(new SendResponse(false, "Error: " + e.getMessage()));
        }
    }

    private void handleMessages(Context ctx) {
        try {
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

            List<String> rawMessages = restSender.pollMessages(Protocol.REST_LONG_POLL_TIMEOUT_MS);

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

        } catch (InterruptedException e) {
            ctx.json(new MessagesResponse(false, new ArrayList<>()));
        } catch (Exception e) {
            ctx.json(new MessagesResponse(false, new ArrayList<>()));
        }
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
