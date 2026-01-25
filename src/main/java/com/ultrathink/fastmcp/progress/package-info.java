/**
 * Progress tracking for long-running operations.
 * Sends progress notifications to MCP clients during execution.
 *
 * <p>Usage example:
 * <pre>{@code
 * @McpTool(description = "Process file")
 * @McpAsync
 * public Mono<String> processFile(
 *     String fileId,
 *     @McpProgress ProgressReporter progress
 * ) {
 *     return Mono.fromCallable(() -> {
 *         progress.report(0, "Starting...");
 *         // do work
 *         progress.report(50, "Half done...");
 *         // finish work
 *         progress.complete();
 *         return "Done";
 *     });
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
package com.ultrathink.fastmcp.progress;
