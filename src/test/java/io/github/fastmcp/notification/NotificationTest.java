package io.github.fastmcp.notification;

import io.github.fastmcp.notification.ResourceChangedNotification.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testLoggingNotification() {
        LoggingNotification notification = new LoggingNotification("info", "test", "data");
        assertEquals("info", notification.level());
        assertEquals("test", notification.logger());
        assertEquals("data", notification.data());
    }

    @Test
    void testProgressNotification() {
        ProgressNotification notification = new ProgressNotification(1.0, 0.5, "Processing");
        assertEquals(1.0, notification.progressToken());
        assertEquals(0.5, notification.progress());
        assertEquals("Processing", notification.message());
    }
    
    @Test
    void testProgressNotificationWithTotal() {
        ProgressNotification notification = new ProgressNotification("token-123", 50.0, 100.0, "Halfway");
        assertEquals("token-123", notification.progressToken());
        assertEquals(50.0, notification.progress());
        assertEquals(100.0, notification.total());
        assertEquals("Halfway", notification.message());
    }
    
    @Test
    void testProgressNotificationFactoryMethods() {
        ProgressNotification withoutTotal = ProgressNotification.of("token", 30.0, "Working");
        assertEquals("token", withoutTotal.progressToken());
        assertEquals(30.0, withoutTotal.progress());
        assertNull(withoutTotal.total());
        
        ProgressNotification withTotal = ProgressNotification.of("token", 60.0, 100.0, "Almost done");
        assertEquals(60.0, withTotal.progress());
        assertEquals(100.0, withTotal.total());
    }

    @Test
    void testResourceChangedNotification() {
        ResourceChangedNotification notification = new ResourceChangedNotification("file://test.txt", ResourceType.CREATE);
        assertEquals("file://test.txt", notification.uri());
        assertEquals(ResourceType.CREATE, notification.type());
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
