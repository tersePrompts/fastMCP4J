/**
 * Icon support for MCP tools, resources, and prompts.
 * Icons can be emoji (Unicode), URLs (http/https), or data URIs.
 *
 * <p>Icon formats supported:
 * <ul>
 *   <li>Emoji: "🔍", "📁", "⚙️"</li>
 *   <li>URL: "https://example.com/icon.png"</li>
 *   <li>Data URI: "data:image/svg+xml;base64,..."</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @McpTool(
 *     name = "search",
 *     description = "Search files",
 *     icon = "🔍"
 * )
 * public String search(String query) { ... }
 * }</pre>
 *
 * @since 0.2.0
 */
package io.github.terseprompts.fastmcp.icons;
