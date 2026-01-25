package io.github.fastmcp.notification;

public class ResourceChangedNotification {
    private final String uri;
    private final ResourceType type;

    public ResourceChangedNotification(String uri, ResourceType type) {
        this.uri = uri;
        this.type = type;
    }

    public String uri() { return uri; }
    public ResourceType type() { return type; }

    public enum ResourceType {
        CREATE,
        UPDATE,
        DELETE
    }
}
