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
    ERROR,
    LIST_ROOMS,
    ROOM_LIST,
    LIST_USERS,
    USER_LIST
}
