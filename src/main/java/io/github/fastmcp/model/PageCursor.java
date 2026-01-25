package io.github.fastmcp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Objects;

public class PageCursor {
    private final int offset;
    private final int limit;
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_PAGE_SIZE = 50;
    
    public PageCursor(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }
    
    public int offset() { return offset; }
    public int limit() { return limit; }
    
    public static PageCursor first() {
        return new PageCursor(0, DEFAULT_PAGE_SIZE);
    }
    
    public static PageCursor parse(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return first();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursor);
            return MAPPER.readValue(decoded, PageCursor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
    
    public String encode() {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(this);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to encode cursor", e);
        }
    }
    
    public PageCursor next() {
        return new PageCursor(offset + limit, limit);
    }
    
    public static PageCursor of(int offset, int limit) {
        return new PageCursor(offset, limit);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageCursor that = (PageCursor) o;
        return offset == that.offset && limit == that.limit;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(offset, limit);
    }
    
    @Override
    public String toString() {
        return "PageCursor[offset=" + offset + ", limit=" + limit + "]";
    }
}
