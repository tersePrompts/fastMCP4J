package com.ultrathink.fastmcp.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.exception.FastMcpException;
import com.ultrathink.fastmcp.notification.ResourceChangedNotification.ResourceType;

import java.util.function.Consumer;

public class NotificationSender {
    private final Consumer<Object> sendFunction;
    private final ObjectMapper mapper = new ObjectMapper();

    public NotificationSender(Consumer<Object> sendFunction) {
        this.sendFunction = sendFunction;
    }

    public void log(String level, String logger, String data) {
        LoggingNotification notification = new LoggingNotification(level, logger, data);
        sendNotification("notifications/logging", notification);
    }

    public void progress(double token, double progress, String message) {
        ProgressNotification notification = new ProgressNotification(token, progress, message);
        sendNotification("notifications/progress", notification);
    }

    public void resourceChanged(String uri, ResourceType type) {
        ResourceChangedNotification notification = new ResourceChangedNotification(uri, type);
        sendNotification("notifications/resources/updated", notification);
    }
    
    /**
     * Send a log message notification (for Context API).
     * @param level Log level (debug, info, warning, error)
     * @param message Log message
     */
    public void sendLogMessage(String level, String message) {
        log(level, "fastmcp", message);
    }
    
    /**
     * Send a progress notification (for Context API).
     * @param progress Current progress value
     * @param total Total value
     * @param message Optional progress message
     */
    public void sendProgressNotification(double progress, double total, String message) {
        double percentage = total > 0 ? (progress / total) * 100 : 0;
        progress(progress, percentage, message);
    }
    
    /**
     * Send a resource change notification (for Context API).
     * @param action Resource action (read, write, delete)
     * @param uri Resource URI
     */
    public void sendResourceChangeNotification(String action, String uri) {
        // Convert action to ResourceType for existing API
        ResourceType type = switch (action.toLowerCase()) {
            case "delete" -> ResourceType.DELETE;
            default -> ResourceType.UPDATE;
        };
        resourceChanged(uri, type);
    }

    private void sendNotification(String method, Object notification) {
        try {
            String json = mapper.writeValueAsString(notification);
            sendFunction.accept(json);
        } catch (Exception e) {
            throw new FastMcpException("Failed to send notification", e);
        }
    }
}
