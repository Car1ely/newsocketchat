package org.example;

import org.example.client.ChatClient;
import org.example.protocol.Protocol;
import org.example.server.ChatServer;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "server":
                runServer(args);
                break;
            case "client":
                runClient(args);
                break;
            case "rest":
                runRestServer(args);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                printUsage();
        }
    }

    private static void runServer(String[] args) {
        int port = Protocol.DEFAULT_PORT;

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + Protocol.DEFAULT_PORT);
            }
        }

        ChatServer.main(new String[]{String.valueOf(port)});
    }

    private static void runClient(String[] args) {
        String host = "localhost";
        int port = Protocol.DEFAULT_PORT;

        if (args.length > 1) {
            host = args[1];
        }
        if (args.length > 2) {
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + Protocol.DEFAULT_PORT);
            }
        }

        ChatClient.main(new String[]{host, String.valueOf(port)});
    }

    private static void runRestServer(String[] args) {
        int port = Protocol.DEFAULT_REST_PORT;

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Using default: " + Protocol.DEFAULT_REST_PORT);
            }
        }

        org.example.server.rest.RestServer.main(new String[]{String.valueOf(port)});
    }

    private static void printUsage() {
        System.out.println("Multi-Protocol Chat System");
        System.out.println("\nUsage:");
        System.out.println("  java -cp target/classes org.example.Main server [port]");
        System.out.println("  java -cp target/classes org.example.Main client [host] [port]");
        System.out.println("  java -cp target/classes org.example.Main rest [port]");
        System.out.println("\nExamples:");
        System.out.println("  java -cp target/classes org.example.Main server 8888");
        System.out.println("  java -cp target/classes org.example.Main client localhost 8888");
        System.out.println("  java -cp target/classes org.example.Main rest 8890");
        System.out.println("\nDefault ports:");
        System.out.println("  TCP: " + Protocol.DEFAULT_PORT);
        System.out.println("  REST: " + Protocol.DEFAULT_REST_PORT);
    }
}