package com.ultrathink.fastmcp.agent.lifecycle;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages session lifecycle for agent bundles.
 * Handles session bootstrap, expiry tracking, and lifecycle hooks.
 */
@Slf4j
public class SessionLifecycle {

    /** Lifecycle phases */
    public enum LifecyclePhase {
        BOOTSTRAP, ACTIVE, EXPIRING, TERMINATED
    }

    /** Expiry reasons */
    public enum ExpiryReason {
        TIMEOUT("session_timeout"),
        MANUAL("manual_expiry"),
        SERVER_SHUTDOWN("server_shutdown");

        private final String code;
        ExpiryReason(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    /** End reasons */
    public enum EndReason {
        EXPIRED("expired"),
        CLIENT_CLOSED("client_closed"),
        ERROR("error"),
        MANUAL("manual");

        private final String code;
        EndReason(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    /** Session state */
    public static class SessionState {
        private final String sessionId;
        private final String conversationId;
        private final Instant createdAt;
        private final Map<String, Object> metadata;
        private LifecyclePhase phase;
        private Instant expiresAt;
        private ExpiryReason expiryReason;

        public SessionState(String sessionId, String conversationId, LifecyclePhase phase) {
            this.sessionId = sessionId;
            this.conversationId = conversationId;
            this.createdAt = Instant.now();
            this.metadata = new ConcurrentHashMap<>();
            this.phase = phase;
            // Default 1 hour expiry
            this.expiresAt = Instant.now().plusSeconds(3600);
        }

        public String getSessionId() { return sessionId; }
        public String getConversationId() { return conversationId; }
        public Instant getCreatedAt() { return createdAt; }
        public Map<String, Object> getMetadata() { return metadata; }
        public LifecyclePhase getPhase() { return phase; }
        public Instant getExpiresAt() { return expiresAt; }
        public ExpiryReason getExpiryReason() { return expiryReason; }

        public void transitionTo(LifecyclePhase newPhase) {
            this.phase = newPhase;
        }

        public void setExpiresAt(Instant expiresAt, ExpiryReason reason) {
            this.expiresAt = expiresAt;
            this.expiryReason = reason;
        }

        public boolean isExpiredAt(Instant timestamp) {
            return !timestamp.isBefore(expiresAt);
        }
    }

    /** Bootstrap configuration */
    public static class BootstrapConfig {
        private final String tenantId;
        private final String userId;
        private final String persona;
        private final java.time.Duration timeout;

        public BootstrapConfig(String tenantId, String userId, String persona, java.time.Duration timeout) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.persona = persona;
            this.timeout = timeout;
        }

        public static Builder builder() { return new Builder(); }

        public String getTenantId() { return tenantId; }
        public String getUserId() { return userId; }
        public String getPersona() { return persona; }
        public java.time.Duration getTimeout() { return timeout; }

        public static class Builder {
            private String tenantId = "default";
            private String userId;
            private String persona = "default";
            private java.time.Duration timeout = java.time.Duration.ofHours(1);

            public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
            public Builder userId(String userId) { this.userId = userId; return this; }
            public Builder persona(String persona) { this.persona = persona; return this; }
            public Builder timeout(java.time.Duration timeout) { this.timeout = timeout; return this; }

            public BootstrapConfig build() {
                return new BootstrapConfig(tenantId, userId, persona, timeout);
            }
        }
    }

    private final Map<String, SessionState> activeSessions = new ConcurrentHashMap<>();
    private final List<SessionLifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private java.time.Duration defaultTimeout = java.time.Duration.ofHours(1);

    /**
     * Bootstrap a new session
     */
    public SessionState bootstrapSession(String sessionId, BootstrapConfig config) {
        String conversationId = UUID.randomUUID().toString();
        SessionState state = new SessionState(sessionId, conversationId, LifecyclePhase.BOOTSTRAP);

        // Set timeout
        state.setExpiresAt(Instant.now().plus(config.getTimeout()), ExpiryReason.TIMEOUT);

        activeSessions.put(sessionId, state);

        log.info("Bootstrapped session {} (conversationId: {})", sessionId, conversationId);

        // Notify listeners
        notifyBootstrap(sessionId, config);

        state.transitionTo(LifecyclePhase.ACTIVE);
        notifySessionStart(sessionId);

        return state;
    }

    /**
     * Bootstrap a session with defaults
     */
    public SessionState bootstrapSession(String sessionId) {
        return bootstrapSession(sessionId, BootstrapConfig.builder().build());
    }

    /**
     * Get a session by ID
     */
    public SessionState getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Check and expire sessions
     */
    public void checkExpiry() {
        Instant now = Instant.now();
        activeSessions.entrySet().removeIf(entry -> {
            SessionState state = entry.getValue();
            if (state.isExpiredAt(now)) {
                log.info("Session {} expired at {}", entry.getKey(), state.getExpiresAt());
                notifySessionExpiring(entry.getKey(), state.getExpiryReason());
                notifySessionEnd(entry.getKey(), EndReason.EXPIRED);
                return true;
            }
            return false;
        });
    }

    /**
     * Manually terminate a session
     */
    public void terminateSession(String sessionId, EndReason reason) {
        SessionState state = activeSessions.remove(sessionId);
        if (state != null) {
            state.transitionTo(LifecyclePhase.TERMINATED);
            notifySessionEnd(sessionId, reason);
            log.info("Terminated session {} (reason: {})", sessionId, reason);
        }
    }

    /**
     * Add a lifecycle listener
     */
    public void addListener(SessionLifecycleListener listener) {
        listeners.add(listener);
    }

    private void notifyBootstrap(String sessionId, BootstrapConfig config) {
        listeners.forEach(l -> {
            try {
                l.onSessionBootstrap(sessionId, config);
            } catch (Exception e) {
                log.error("Listener error in onSessionBootstrap", e);
            }
        });
    }

    private void notifySessionStart(String sessionId) {
        listeners.forEach(l -> {
            try {
                l.onSessionStart(sessionId);
            } catch (Exception e) {
                log.error("Listener error in onSessionStart", e);
            }
        });
    }

    private void notifySessionExpiring(String sessionId, ExpiryReason reason) {
        listeners.forEach(l -> {
            try {
                l.onSessionExpiring(sessionId, reason);
            } catch (Exception e) {
                log.error("Listener error in onSessionExpiring", e);
            }
        });
    }

    private void notifySessionEnd(String sessionId, EndReason reason) {
        listeners.forEach(l -> {
            try {
                l.onSessionEnd(sessionId, reason);
            } catch (Exception e) {
                log.error("Listener error in onSessionEnd", e);
            }
        });
    }

    /**
     * Get default timeout
     */
    public java.time.Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Set default timeout
     */
    public void setDefaultTimeout(java.time.Duration timeout) {
        this.defaultTimeout = timeout;
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Get all active sessions
     */
    public Map<String, SessionState> getActiveSessions() {
        return Map.copyOf(activeSessions);
    }

    /**
     * Session lifecycle listener interface
     */
    public interface SessionLifecycleListener {
        default void onSessionBootstrap(String sessionId, BootstrapConfig config) {}
        default void onSessionStart(String sessionId) {}
        default void onSessionExpiring(String sessionId, ExpiryReason reason) {}
        default void onSessionEnd(String sessionId, EndReason reason) {}
    }
}
