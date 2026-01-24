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

    private void sendNotification(String method, Object notification) {
        try {
            String json = mapper.writeValueAsString(notification);
            sendFunction.accept(json);
        } catch (Exception e) {
            throw new FastMcpException("Failed to send notification", e);
        }
    }
}
