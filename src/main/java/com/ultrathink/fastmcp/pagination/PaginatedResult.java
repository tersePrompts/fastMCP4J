package com.ultrathink.fastmcp.pagination;

import java.util.List;
import java.util.Optional;

/**
 * Wrapper for paginated results following MCP cursor-based pagination.
 *
 * @param <T> type of items in the result
 * @version 0.2.0
 * @status NOT_IMPLEMENTED
 */
public record PaginatedResult<T>(
    List<T> items,
    String nextCursor
) {

    /**
     * Create paginated result with items and next cursor
     */
    public static <T> PaginatedResult<T> of(List<T> items, String nextCursor) {
        return new PaginatedResult<>(items, nextCursor);
    }

    /**
     * Create final page result (no more results)
     */
    public static <T> PaginatedResult<T> lastPage(List<T> items) {
        return new PaginatedResult<>(items, null);
    }

    /**
     * Check if there are more results available
     */
    public boolean hasMore() {
        return nextCursor != null && !nextCursor.isEmpty();
    }

    /**
     * Get next cursor as Optional
     */
    public Optional<String> getNextCursor() {
        return Optional.ofNullable(nextCursor);
    }
}
