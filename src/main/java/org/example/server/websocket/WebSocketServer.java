package org.example.server.websocket;

import org.example.protocol.Protocol;
import org.example.server.RoomManager;
import org.glassfish.tyrus.server.Server;

public class WebSocketServer {
    private final int port;
    private final RoomManager roomManager;
    private Server server;

    public WebSocketServer(int port) {
        this.port = port;
        this.roomManager = new RoomManager();
    }

    public void start() {
        ChatEndpoint.setRoomManager(roomManager);

        server = new Server(
                "localhost",
                port,
                Protocol.WS_CONTEXT_PATH,
                null,
                ChatEndpoint.class
        );

        try {
            server.start();
            System.out.println("WebSocket Server started on ws://localhost:" + port + Protocol.WS_CONTEXT_PATH + "/chat");
            System.out.println("Waiting for clients to connect...");
            System.out.println("Press Ctrl+C to stop...");

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Failed to start WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        System.out.println("\nShutting down WebSocket server...");
        if (server != null) {
            server.stop();
        }
        System.out.println("WebSocket server shutdown complete");
    }

    public static void main(String[] args) {
        int port = Protocol.DEFAULT_WS_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Using default: " + port);
            }
        }

        WebSocketServer server = new WebSocketServer(port);
        server.start();
    }
}
