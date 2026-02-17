package org.example.protocol;

public enum MessageType {
    CONNECT,
    CONNECT_ACK,
    JOIN,
    JOIN_ACK,
    MESSAGE,
    BROADCAST,
    USER_JOINED,
    USER_LEFT,
    DISCONNECT,
    ERROR
}
