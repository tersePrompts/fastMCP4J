/**
 * LLM sampling support for requesting completions from MCP clients.
 * Allows servers to leverage client-side LLM capabilities.
 *
 * <p>Usage example:
 * <pre>{@code
 * @McpTool(description = "Generate SQL")
 * public String generateSql(
 *     String naturalLanguage,
 *     @McpSampler LLMSampler sampler
 * ) {
 *     SamplingRequest request = SamplingRequest.builder()
 *         .prompt("Convert to SQL: " + naturalLanguage)
 *         .maxTokens(200)
 *         .temperature(0.7)
 *         .build();
 *
 *     return sampler.complete(request);
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
package com.ultrathink.fastmcp.sampling;
