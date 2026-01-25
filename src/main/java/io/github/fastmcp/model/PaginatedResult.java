package io.github.fastmcp.model;

import java.util.List;
import java.util.Objects;

public class PaginatedResult<T> {
    private final List<T> items;
    private final String nextCursor;
    
    public PaginatedResult(List<T> items, String nextCursor) {
        this.items = items;
        this.nextCursor = nextCursor;
    }
    
    public List<T> items() { return items; }
    public String nextCursor() { return nextCursor; }
    
    public static <T> PaginatedResult<T> of(List<T> items) {
        return new PaginatedResult<>(items, null);
    }
    
    public static <T> PaginatedResult<T> of(List<T> items, String nextCursor) {
        return new PaginatedResult<>(items, nextCursor);
    }
    
    public boolean hasMore() {
        return nextCursor != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginatedResult<?> that = (PaginatedResult<?>) o;
        return Objects.equals(nextCursor, that.nextCursor) && Objects.equals(items, that.items);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(items, nextCursor);
    }
    
    @Override
    public String toString() {
        return "PaginatedResult[items=" + items.size() + ", nextCursor=" + nextCursor + "]";
    }
}
