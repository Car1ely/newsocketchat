package org.example.server.rest;

import org.example.server.model.User;
import org.example.server.RoomManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final ConcurrentHashMap<String, User> sessions;
    private final ConcurrentHashMap<String, Long> lastActivity;
    private final RoomManager roomManager;
    private final ScheduledExecutorService cleanupExecutor;

    private static final long SESSION_TIMEOUT_MS = 300000;
    private static final long CLEANUP_INTERVAL_MS = 60000;

    public SessionManager(RoomManager roomManager) {
        this.sessions = new ConcurrentHashMap<>();
        this.lastActivity = new ConcurrentHashMap<>();
        this.roomManager = roomManager;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask();
    }

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        updateActivity(sessionId);
        return sessionId;
    }

    public User getUser(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        User user = sessions.get(sessionId);
        if (user != null) {
            updateActivity(sessionId);
        }
        return user;
    }

    public void removeSession(String sessionId) {
        User user = sessions.remove(sessionId);
        lastActivity.remove(sessionId);

        if (user != null) {
            roomManager.removeUserFromRoom(user);
            roomManager.unregisterUser(user);
            user.getMessageSender().close();
        }
    }

    private void updateActivity(String sessionId) {
        lastActivity.put(sessionId, System.currentTimeMillis());
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            CLEANUP_INTERVAL_MS,
            CLEANUP_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();

        lastActivity.forEach((sessionId, timestamp) -> {
            if (now - timestamp > SESSION_TIMEOUT_MS) {
                System.out.println("Session expired: " + sessionId);
                removeSession(sessionId);
            }
        });
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
        }

        sessions.keySet().forEach(this::removeSession);
    }

}
