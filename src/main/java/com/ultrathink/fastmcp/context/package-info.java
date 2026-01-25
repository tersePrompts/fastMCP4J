/**
 * MCP request context support.
 * Provides access to client information, request metadata, and session data.
 *
 * <p>Usage example:
 * <pre>{@code
 * @McpTool(description = "Get client info")
 * public String getClientInfo(@McpContext RequestContext context) {
 *     return "Client: " + context.getClientName();
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
package com.ultrathink.fastmcp.context;
