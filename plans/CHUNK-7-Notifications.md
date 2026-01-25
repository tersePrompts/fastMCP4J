# CHUNK 7: Notifications

**Dependencies**: CHUNK 0-6

**Files**:
- `src/main/java/io/github/fastmcp/notification/NotificationType.java`
- `src/main/java/io/github/fastmcp/notification/LoggingNotification.java`
- `src/main/java/io/github/fastmcp/notification/ProgressNotification.java`
- `src/main/java/io/github/fastmcp/notification/ResourceChangedNotification.java`
- `src/main/java/io/github/fastmcp/notification/NotificationSender.java`
- `src/test/java/io/github/fastmcp/notification/NotificationTest.java`

## Implementation

### NotificationType.java
```java
package com.ultrathink.fastmcp.notification;

public enum NotificationType {
    LOGGING,
    PROGRESS,
    RESOURCE_CHANGED
}
```

### LoggingNotification.java
```java
package com.ultrathink.fastmcp.notification;

import lombok.Value;

@Value
public class LoggingNotification {
    String level;  // info, warning, error
    String logger;
    String data;
}
```

### ProgressNotification.java
```java
package com.ultrathink.fastmcp.notification;

import lombok.Value;

@Value
public class ProgressNotification {
    double progressToken;
    double progress;
    String message;
}
```

### ResourceChangedNotification.java
```java
package com.ultrathink.fastmcp.notification;

import lombok.Value;

@Value
public class ResourceChangedNotification {
    String uri;
    ResourceType type;
}

enum ResourceType {
    CREATE,
    UPDATE,
    DELETE
}
```

### NotificationSender.java

```java
package com.ultrathink.fastmcp.notification;

import exception.com.ultrathink.fastmcp.FastMcpException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.notification.LoggingNotification;
import com.ultrathink.fastmcp.notification.ProgressNotification;
import com.ultrathink.fastmcp.notification.ResourceChangedNotification;

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
```

### Add to FastMCP.java
```java
// Add to FastMCP class
private NotificationSender notificationSender;

public FastMCP withNotificationSender(Consumer<Object> sendFunction) {
    this.notificationSender = new NotificationSender(sendFunction);
    return this;
}

public NotificationSender getNotificationSender() {
    return notificationSender;
}
```

## Tests

```java
package com.ultrathink.fastmcp.notification;

import com.ultrathink.fastmcp.notification.LoggingNotification;
import com.ultrathink.fastmcp.notification.ProgressNotification;
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
}
```
