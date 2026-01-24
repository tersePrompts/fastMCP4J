package io.github.fastmcp.notification;

import lombok.Value;

@Value
public class ResourceChangedNotification {
    String uri;
    ResourceType type;

    public enum ResourceType {
        CREATE,
        UPDATE,
        DELETE
    }
}
