package org.example.client;

import org.example.protocol.Message;
import org.example.protocol.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private volatile boolean running;

    public ChatClient() {
        this.running = true;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Connected to server at " + host + ":" + port);
    }

    public void initialize() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your nickname: ");
        nickname = scanner.nextLine().trim();

        Message connectMsg = Message.createConnect(nickname);
        out.println(connectMsg.serialize());

        String response;
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.err.println("Failed to read server response: " + e.getMessage());
            running = false;
            return;
        }

        if (response == null) {
            System.err.println("Server closed connection");
            running = false;
            return;
        }

        Message ack = Message.parse(response);
        if (ack.getType() == org.example.protocol.MessageType.CONNECT_ACK) {
            boolean success = Boolean.parseBoolean(ack.getParam(0));
            String message = ack.getParam(1);

            if (success) {
                System.out.println(message);
                System.out.println("Type /join <room> to join a room");
                System.out.println("Type /quit to exit");
            } else {
                System.err.println("Connection failed: " + message);
                running = false;
                return;
            }
        }

        Thread listenerThread = new Thread(new ServerListener(in, this));
        Thread inputThread = new Thread(new InputHandler(out, this));

        listenerThread.start();
        inputThread.start();

        try {
            listenerThread.join();
            inputThread.join();
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

    public void shutdown() {
        System.out.println("Disconnecting...");
        running = false;

        if (out != null) {
            Message disconnectMsg = Message.createDisconnect();
            out.println(disconnectMsg.serialize());
            out.close();
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("Error closing input stream: " + e.getMessage());
            }
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }

        System.out.println("Disconnected from server");
    }

    public boolean isRunning() {
        return running;
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = Protocol.DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + Protocol.DEFAULT_PORT);
            }
        }

        ChatClient client = new ChatClient();

        try {
            client.connect(host, port);
            client.initialize();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.exit(1);
        } finally {
            client.shutdown();
        }
    }
}
