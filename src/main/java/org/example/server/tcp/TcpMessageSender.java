package org.example.server.tcp;

import org.example.protocol.MessageSender;

import java.io.PrintWriter;

public class TcpMessageSender implements MessageSender {
    private final PrintWriter out;
    private volatile boolean connected = true;

    public TcpMessageSender(PrintWriter out) {
        if (out == null) {
            throw new IllegalArgumentException("PrintWriter cannot be null");
        }
        this.out = out;
    }

    @Override
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
            out.flush();

            if (out.checkError()) {
                connected = false;
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected && out != null && !out.checkError();
    }

    @Override
    public void close() {
        connected = false;
        if (out != null) {
            out.close();
        }
    }
}
