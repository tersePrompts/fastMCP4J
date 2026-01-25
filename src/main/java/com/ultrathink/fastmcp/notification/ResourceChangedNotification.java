package com.ultrathink.fastmcp.notification;

import lombok.Data;

@Data
public class ResourceChangedNotification {
    String uri;
    ResourceType type;

    public enum ResourceType {
        CREATE,
        UPDATE,
        DELETE
    }
    
    public ResourceChangedNotification() {}
    
    public ResourceChangedNotification(String uri, ResourceType type) {
        this.uri = uri;
        this.type = type;
    }
}
