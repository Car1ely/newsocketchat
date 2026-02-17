package org.example.client;

import org.example.protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class InputHandler implements Runnable {
    private final PrintWriter out;
    private final ChatClient client;
    private final BufferedReader consoleReader;

    public InputHandler(PrintWriter out, ChatClient client) {
        this.out = out;
        this.client = client;
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        try {
            String input;
            while (client.isRunning() && (input = consoleReader.readLine()) != null) {
                processInput(input.trim());
            }
        } catch (IOException e) {
            if (client.isRunning()) {
                System.err.println("Error reading console input: " + e.getMessage());
            }
        }
    }

    private void processInput(String input) {
        if (input.isEmpty()) {
            return;
        }

        if (input.startsWith("/")) {
            handleCommand(input);
        } else {
            sendMessage(input);
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/join":
                if (parts.length < 2) {
                    System.err.println("Usage: /join <room>");
                } else {
                    sendJoin(parts[1]);
                }
                break;
            case "/quit":
            case "/exit":
                client.shutdown();
                break;
            case "/help":
                showHelp();
                break;
            default:
                System.err.println("Unknown command: " + cmd);
                System.err.println("Type /help for available commands");
        }
    }

    private void sendMessage(String text) {
        Message msg = Message.createMessage(text);
        out.println(msg.serialize());
    }

    private void sendJoin(String roomName) {
        Message msg = Message.createJoin(roomName);
        out.println(msg.serialize());
    }

    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  /join <room>  - Join a chat room");
        System.out.println("  /quit         - Disconnect and exit");
        System.out.println("  /help         - Show this help message");
        System.out.println("\nType any text to send a message to the current room\n");
    }
}
