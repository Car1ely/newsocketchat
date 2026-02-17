package org.example.client.websocket;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.Protocol;
import jakarta.websocket.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private String nickname;
    private volatile boolean running = true;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to WebSocket server");
    }

    @OnMessage
    public void onMessage(String messageJson) {
        try {
            Message message = Message.fromJson(messageJson);
            handleMessage(message);
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Disconnected: " + reason.getReasonPhrase());
        running = false;
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    private void handleMessage(Message message) {
        MessageType type = message.getType();

        switch (type) {
            case CONNECT_ACK:
                boolean success = Boolean.parseBoolean(message.getParam(0));
                String msg = message.getParam(1);
                if (success) {
                    System.out.println(msg);
                    System.out.println("Type /join <room> to join a room");
                    System.out.println("Type /quit to exit");
                } else {
                    System.err.println("Connection failed: " + msg);
                    running = false;
                }
                break;

            case BROADCAST:
                String sender = message.getParam(0);
                String text = message.getParam(1);
                System.out.println("[" + sender + "]: " + text);
                break;

            case USER_JOINED:
                String joinedUser = message.getParam(0);
                System.out.println("* " + joinedUser + " joined the room");
                break;

            case USER_LEFT:
                String leftUser = message.getParam(0);
                System.out.println("* " + leftUser + " left the room");
                break;

            case JOIN_ACK:
                String roomName = message.getParam(0);
                boolean joinSuccess = Boolean.parseBoolean(message.getParam(1));
                if (joinSuccess) {
                    System.out.println("Joined room: " + roomName);
                } else {
                    System.err.println("Failed to join room: " + roomName);
                }
                break;

            case ERROR:
                String error = message.getParam(0);
                System.err.println("Error: " + error);
                break;

            default:
                System.out.println("Received unknown message type: " + type);
        }
    }

    public void connect(String host, int port) throws Exception {
        String uri = "ws://" + host + ":" + port + Protocol.WS_CONTEXT_PATH + Protocol.WS_ENDPOINT_PATH;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, new URI(uri));
    }

    public void sendMessage(Message message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message.toJson());
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    public void startInputLoop() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your nickname: ");
        nickname = scanner.nextLine().trim();

        sendMessage(Message.createConnect(nickname));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (running && scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.startsWith("/")) {
                handleCommand(input);
            } else {
                sendMessage(Message.createMessage(input));
            }
        }

        scanner.close();
    }

    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/join":
                if (parts.length < 2) {
                    System.err.println("Usage: /join <room>");
                } else {
                    sendMessage(Message.createJoin(parts[1]));
                }
                break;

            case "/quit":
            case "/exit":
                sendMessage(Message.createDisconnect());
                running = false;
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        System.err.println("Error closing session: " + e.getMessage());
                    }
                }
                break;

            case "/help":
                showHelp();
                break;

            default:
                System.err.println("Unknown command: " + cmd);
                System.err.println("Type /help for available commands");
        }
    }

    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  /join <room>  - Join a chat room");
        System.out.println("  /quit         - Disconnect and exit");
        System.out.println("  /help         - Show this help message");
        System.out.println("\nType any text to send a message to the current room\n");
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = Protocol.DEFAULT_WS_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Using default: " + Protocol.DEFAULT_WS_PORT);
            }
        }

        WebSocketClient client = new WebSocketClient();

        try {
            client.connect(host, port);
            client.startInputLoop();
        } catch (Exception e) {
            System.err.println("Failed to connect to WebSocket server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
