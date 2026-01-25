/**
 * Pagination support for large result sets.
 * Provides cursor-based pagination following MCP specification.
 *
 * <p>Usage example:
 * <pre>{@code
 * @McpTool(description = "List users")
 * public PaginatedResult<User> listUsers(
 *     @McpParam(required = false) String cursor,
 *     @McpParam(defaultValue = "50", required = false) int limit
 * ) {
 *     List<User> users = fetchUsers(cursor, limit);
 *     String nextCursor = hasMore() ? generateCursor() : null;
 *     return PaginatedResult.of(users, nextCursor);
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
package com.ultrathink.fastmcp.pagination;
