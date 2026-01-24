package io.github.fastmcp.notification;

import io.github.fastmcp.notification.ResourceChangedNotification.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testLoggingNotification() {
        LoggingNotification notification = new LoggingNotification("info", "test", "data");
        assertEquals("info", notification.getLevel());
        assertEquals("test", notification.getLogger());
        assertEquals("data", notification.getData());
    }

    @Test
    void testProgressNotification() {
        ProgressNotification notification = new ProgressNotification(1.0, 0.5, "Processing");
        assertEquals(1.0, notification.getProgressToken());
        assertEquals(0.5, notification.getProgress());
        assertEquals("Processing", notification.getMessage());
    }

    @Test
    void testResourceChangedNotification() {
        ResourceChangedNotification notification = new ResourceChangedNotification("file://test.txt", ResourceType.CREATE);
        assertEquals("file://test.txt", notification.getUri());
        assertEquals(ResourceType.CREATE, notification.getType());
    }

    @Test
    void testNotificationType() {
        NotificationType[] types = NotificationType.values();
        assertEquals(3, types.length);
        assertEquals(NotificationType.LOGGING, types[0]);
        assertEquals(NotificationType.PROGRESS, types[1]);
        assertEquals(NotificationType.RESOURCE_CHANGED, types[2]);
    }
}
