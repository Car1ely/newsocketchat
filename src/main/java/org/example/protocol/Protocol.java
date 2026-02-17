package org.example.protocol;

public class Protocol {
    public static final String DELIMITER = "|";
    public static final String LINE_TERMINATOR = "\n";
    public static final int DEFAULT_PORT = 8888;

    public static final int DEFAULT_WS_PORT = 8889;
    public static final String WS_CONTEXT_PATH = "/ws";
    public static final String WS_ENDPOINT_PATH = "/chat";
    public static final long WS_IDLE_TIMEOUT = 300000;
    public static final int WS_MAX_BUFFER_SIZE = 8192;

    public static final int DEFAULT_REST_PORT = 8890;
    public static final String REST_BASE_PATH = "/api";
    public static final long REST_LONG_POLL_TIMEOUT_MS = 30000;
    public static final int REST_MESSAGE_QUEUE_SIZE = 100;

    private Protocol() {
    }
}
