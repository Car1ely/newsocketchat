package org.example.server;

import org.example.protocol.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private final int port;
    private final RoomManager roomManager;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(int port) {
        this.port = port;
        this.roomManager = new RoomManager();
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Chat Server started on port " + port);
            System.out.println("Waiting for clients to connect...");

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, roomManager);
                    Thread clientThread = new Thread(handler);
                    clientThread.start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        System.out.println("\nShutting down server...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println("Server shutdown complete");
    }

    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + Protocol.DEFAULT_PORT);
            }
        }

        ChatServer server = new ChatServer(port);
        server.start();
    }
}
