package com.ultrathink.fastmcp.agent.memory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enhanced memory entry with type, scope, and metadata.
 */
public class MemoryEntry {

    private final String id;
    private final MemoryType type;
    private final MemoryScope scope;
    private final String content;
    private final Map<String, Object> metadata;
    private final double importance;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final List<String> tags;

    private MemoryEntry(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.scope = builder.scope;
        this.content = builder.content;
        this.metadata = Map.copyOf(builder.metadata);
        this.importance = builder.importance;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.tags = List.copyOf(builder.tags);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public MemoryType getType() { return type; }
    public MemoryScope getScope() { return scope; }
    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public double getImportance() { return importance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public List<String> getTags() { return tags; }

    public static class Builder {
        private String id = java.util.UUID.randomUUID().toString();
        private MemoryType type = MemoryType.WORKING;
        private MemoryScope scope = MemoryScope.SESSION;
        private String content;
        private Map<String, Object> metadata = new java.util.HashMap<>();
        private double importance = 0.5;
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private List<String> tags = new java.util.ArrayList<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(MemoryType type) { this.type = type; return this; }
        public Builder scope(MemoryScope scope) { this.scope = scope; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) this.metadata = new java.util.HashMap<>(metadata);
            return this;
        }
        public Builder importance(double importance) { this.importance = importance; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder tags(List<String> tags) {
            if (tags != null) this.tags = new java.util.ArrayList<>(tags);
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder putMeta(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public MemoryEntry build() {
            if (content == null) {
                throw new IllegalArgumentException("content is required");
            }
            if (expiresAt == null) {
                // Default: 1 hour expiry
                expiresAt = createdAt.plusSeconds(3600);
            }
            return new MemoryEntry(this);
        }
    }
}
