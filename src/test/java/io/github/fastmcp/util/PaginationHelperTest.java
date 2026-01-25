package io.github.fastmcp.util;

import io.github.fastmcp.model.PageCursor;
import io.github.fastmcp.model.PaginatedResult;
import io.github.fastmcp.notification.NotificationSender;
import io.github.fastmcp.notification.ProgressContext;
import io.github.fastmcp.notification.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class PaginationHelperTest {
    
    private NotificationSender mockSender;
    private ProgressTracker tracker;
    private PaginationHelper helper;
    private ConcurrentLinkedQueue<Object> sentNotifications;
    
    @BeforeEach
    void setUp() {
        sentNotifications = new ConcurrentLinkedQueue<>();
        mockSender = new NotificationSender(sentNotifications::add);
        tracker = new ProgressTracker(mockSender);
        helper = new PaginationHelper(tracker, 3);
    }
    
    @Test
    void testPaginate_FirstPage_HasNextCursor() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        PaginatedResult<String> result = helper.paginate(items, null);
        
        assertEquals(3, result.items().size());
        assertEquals(List.of("a", "b", "c"), result.items());
        assertNotNull(result.nextCursor());
        assertTrue(result.hasMore());
    }
    
    @Test
    void testPaginate_SecondPage_HasNextCursor() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        PaginatedResult<String> first = helper.paginate(items, null);
        PaginatedResult<String> second = helper.paginate(items, first.nextCursor());
        
        assertEquals(2, second.items().size());
        assertEquals(List.of("d", "e"), second.items());
        assertNull(second.nextCursor());
        assertFalse(second.hasMore());
    }
    
    @Test
    void testPaginate_LastPage_NoNextCursor() {
        List<String> items = List.of("a", "b");
        PaginatedResult<String> result = helper.paginate(items, null);
        
        assertEquals(2, result.items().size());
        assertEquals(List.of("a", "b"), result.items());
        assertNull(result.nextCursor());
        assertFalse(result.hasMore());
    }
    
    @Test
    void testPaginate_EmptyList() {
        List<String> items = List.of();
        PaginatedResult<String> result = helper.paginate(items, null);
        
        assertEquals(0, result.items().size());
        assertNull(result.nextCursor());
        assertFalse(result.hasMore());
    }
    
    @Test
    void testPaginate_CustomPageSize() {
        List<String> items = List.of("a", "b", "c", "d", "e", "f");
        PaginatedResult<String> result = helper.paginate(items, null, 2);
        
        assertEquals(2, result.items().size());
        assertEquals(List.of("a", "b"), result.items());
        assertNotNull(result.nextCursor());
    }
    
    @Test
    void testPaginateAndTrack_WithProgress_SendsNotifications() {
        List<String> items = List.of("a", "b", "c", "d", "e", "f");
        ProgressContext ctx = ProgressContext.of("token-123", 0, "Fetching");
        
        PaginatedResult<String> result = helper.paginateAndTrack(items, null, ctx);
        
        assertEquals(3, result.items().size());
        assertFalse(sentNotifications.isEmpty());
        String notification = sentNotifications.peek().toString();
        assertTrue(notification.contains("progress"));
    }
    
    @Test
    void testPaginateAndTrack_CompletesProgress() {
        List<String> items = List.of("a", "b");
        ProgressContext ctx = ProgressContext.of("token-456", 0, "Fetching");
        
        PaginatedResult<String> result = helper.paginateAndTrack(items, null, ctx);
        
        assertEquals(2, result.items().size());
        assertFalse(result.hasMore());
        String lastNotification = sentNotifications.peek().toString();
        assertNotNull(lastNotification);
        assertTrue(lastNotification.contains("progress"));
    }
    
    @Test
    void testPaginateStream_WithCursor() {
        List<String> allItems = List.of("a", "b", "c", "d", "e");
        
        PaginatedResult<String> result = helper.paginateStream(
            cursor -> {
                PageCursor pc = PageCursor.parse(cursor);
                int end = Math.min(pc.offset() + 2, allItems.size());
                List<String> pageItems = allItems.subList(pc.offset(), end);
                String nextCursor = end < allItems.size() ? pc.next().encode() : null;
                return PaginatedResult.of(pageItems, nextCursor);
            },
            null,
            null
        );
        
        assertEquals(2, result.items().size());
        assertEquals(List.of("a", "b"), result.items());
        assertNotNull(result.nextCursor());
    }
}
