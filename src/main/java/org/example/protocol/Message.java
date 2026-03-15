package org.example.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Message {
    private static final Gson gson = new Gson();

    private final MessageType type;
    private final String[] params;

    private Message(MessageType type, String... params) {
        this.type = type;
        this.params = params;
    }

    public MessageType getType() {
        return type;
    }

    public String getParam(int index) {
        if (index >= 0 && index < params.length) {
            return params[index];
        }
        return null;
    }

    public String[] getParams() {
        return params;
    }

    public static Message parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        String[] parts = raw.trim().split("\\" + Protocol.DELIMITER);
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid message format");
        }

        MessageType type;
        try {
            type = MessageType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + parts[0]);
        }

        String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        return new Message(type, params);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());

        for (String param : params) {
            sb.append(Protocol.DELIMITER);
            sb.append(param);
        }

        sb.append(Protocol.LINE_TERMINATOR);
        return sb.toString();
    }

    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());

        JsonArray paramsArray = new JsonArray();
        for (String param : params) {
            paramsArray.add(param);
        }
        json.add("params", paramsArray);

        return gson.toJson(json);
    }

    public static Message fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON cannot be null or empty");
        }

        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);

            String typeStr = obj.get("type").getAsString();
            MessageType type = MessageType.valueOf(typeStr);

            JsonArray paramsArray = obj.getAsJsonArray("params");
            String[] params = new String[paramsArray.size()];
            for (int i = 0; i < paramsArray.size(); i++) {
                params[i] = paramsArray.get(i).getAsString();
            }

            return new Message(type, params);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON message format: " + e.getMessage());
        }
    }

    public static Message createConnect(String nickname) {
        return new Message(MessageType.CONNECT, nickname);
    }

    public static Message createConnectAck(boolean success, String message) {
        return new Message(MessageType.CONNECT_ACK, String.valueOf(success), message);
    }

    public static Message createJoin(String roomName) {
        return new Message(MessageType.JOIN, roomName);
    }

    public static Message createJoinAck(String roomName, boolean success) {
        return new Message(MessageType.JOIN_ACK, roomName, String.valueOf(success));
    }

    public static Message createMessage(String text) {
        return new Message(MessageType.MESSAGE, text);
    }

    public static Message createBroadcast(String sender, String text) {
        return new Message(MessageType.BROADCAST, sender, text);
    }

    public static Message createUserJoined(String nickname) {
        return new Message(MessageType.USER_JOINED, nickname);
    }

    public static Message createUserLeft(String nickname) {
        return new Message(MessageType.USER_LEFT, nickname);
    }

    public static Message createDisconnect() {
        return new Message(MessageType.DISCONNECT);
    }

    public static Message createError(String errorMessage) {
        return new Message(MessageType.ERROR, errorMessage);
    }

    public static Message createListRooms() {
        return new Message(MessageType.LIST_ROOMS);
    }

    public static Message createRoomList(java.util.List<org.example.server.RoomManager.RoomInfo> rooms) {
        String[] params = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            org.example.server.RoomManager.RoomInfo room = rooms.get(i);
            params[i] = room.getName() + ":" + room.getUserCount();
        }
        return new Message(MessageType.ROOM_LIST, params);
    }

    public static Message createListUsers() {
        return new Message(MessageType.LIST_USERS);
    }

    public static Message createUserList(java.util.List<String> users) {
        return new Message(MessageType.USER_LIST, users.toArray(new String[0]));
    }

    public static Message createListHistory() {
        return new Message(MessageType.LIST_HISTORY);
    }

    public static Message createHistoryList(java.util.List<org.example.server.model.ChatMessage> history) {
        String[] params = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            params[i] = history.get(i).format();
        }
        return new Message(MessageType.HISTORY_LIST, params);
    }
}
