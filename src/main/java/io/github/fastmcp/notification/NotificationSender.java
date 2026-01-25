package io.github.fastmcp.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fastmcp.exception.FastMcpException;
import io.github.fastmcp.notification.ResourceChangedNotification.ResourceType;

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
        progress(token, progress, null, message);
    }
    
    public void progress(double token, double progress, Double total, String message) {
        ProgressNotification notification = new ProgressNotification(token, progress, total, message);
        sendNotification("notifications/progress", notification);
    }

    public void resourceChanged(String uri, ResourceType type) {
        ResourceChangedNotification notification = new ResourceChangedNotification(uri, type);
        sendNotification("notifications/resources/updated", notification);
    }

    private void sendNotification(String method, Object notification) {
        try {
            NotificationMessage msg = new NotificationMessage(method, notification);
            String json = mapper.writeValueAsString(msg);
            sendFunction.accept(json);
        } catch (Exception e) {
            throw new FastMcpException("Failed to send notification", e);
        }
    }
    
    private record NotificationMessage(String jsonrpc, String method, Object params) {
        NotificationMessage(String method, Object params) {
            this("2.0", method, params);
        }
    }
}
