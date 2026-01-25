package io.github.fastmcp.notification;

import io.github.fastmcp.exception.FastMcpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTrackerTest {
    
    private NotificationSender mockSender;
    private ProgressTracker tracker;
    private ConcurrentLinkedQueue<Object> sentNotifications;
    
    @BeforeEach
    void setUp() {
        sentNotifications = new ConcurrentLinkedQueue<>();
        mockSender = new NotificationSender(msg -> sentNotifications.add(msg));
        tracker = new ProgressTracker(mockSender);
    }
    
    @Test
    void testUpdate_SendsNotification() {
        tracker.track("token-1");
        tracker.update("token-1", 25.0, "Processing...");
        
        assertFalse(sentNotifications.isEmpty());
        String notification = sentNotifications.peek().toString();
        assertNotNull(notification);
        assertTrue(notification.contains("progress"));
        assertTrue(notification.contains("25"));
    }
    
    @Test
    void testUpdateWithTotal_SendsNotificationWithTotal() {
        tracker.track("token-2");
        tracker.update("token-2", 50.0, 100.0, "Halfway");
        
        String notification = sentNotifications.peek().toString();
        assertNotNull(notification);
        assertTrue(notification.contains("50"));
        assertTrue(notification.contains("100"));
    }
    
    @Test
    void testComplete_SendsFinalProgress() {
        tracker.track("token-3");
        tracker.complete("token-3", "Done!");
        
        String notification = sentNotifications.peek().toString();
        assertNotNull(notification);
        assertTrue(notification.contains("100"));
        assertTrue(notification.contains("Done"));
    }
    
    @Test
    void testComplete_NoMessage_SendsDefault() {
        tracker.track("token-4");
        tracker.complete("token-4");
        
        String notification = sentNotifications.peek().toString();
        assertNotNull(notification);
        assertTrue(notification.contains("100"));
        assertTrue(notification.contains("Completed"));
    }
    
    @Test
    void testFail_SendsErrorMessage() {
        tracker.track("token-5");
        tracker.fail("token-5", "Connection failed");
        
        String notification = sentNotifications.peek().toString();
        assertNotNull(notification);
        assertTrue(notification.contains("Error"));
        assertTrue(notification.contains("Connection failed"));
    }
    
    @Test
    void testUpdateAfterComplete_DoesNotSend() {
        tracker.track("token-6");
        tracker.complete("token-6");
        tracker.update("token-6", 75.0, "Should not send");
        
        assertEquals(1, sentNotifications.size());
    }
    
    @Test
    void testRemove_StopsTracking() {
        tracker.track("token-7");
        tracker.remove("token-7");
        tracker.update("token-7", 50.0, "No notification");
        
        assertTrue(sentNotifications.isEmpty());
    }
    
    @Test
    void testUpdateWithoutTracking_DoesNotSend() {
        tracker.update("untracked-token", 50.0, "No notification");
        
        assertTrue(sentNotifications.isEmpty());
    }
}
