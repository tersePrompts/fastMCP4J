package io.github.terseprompts.fastmcp.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting MCP Context into method parameters.
 * When a method parameter is annotated with @McpContext, the Context
 * object is automatically injected during method invocation.
 * 
 * Context provides access to MCP capabilities like logging, progress reporting,
 * resource/prompt access, LLM sampling, and session state.
 * 
 * Example:
 * <pre>
 * {@code
 * @McpTool(description = "Process file with logging")
 * public String processFile(String path, @McpContext Context ctx) {
 *     ctx.info("Processing: " + path);
 *     // ... processing logic
 *     return "Done";
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface McpContext {
}
