package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a post-hook that executes after a tool invocation.
 *
 * Post-hooks are executed after the target tool method completes, allowing for:
 * - Result transformation
 * - Logging and auditing
 * - Cleanup operations
 * - Side effects (notifications, etc.)
 *
 * Example:
 * <pre>
 * {@code
 * @McpPostHook(toolName = "search", order = 1)
 * public void logSearch(Map<String, Object> args, Object result) {
 *     logger.info("Search completed: query={}, results={}",
 *                 args.get("query"), result);
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpPostHook {
    /**
     * The name of the tool this hook applies to.
     * Use "*" to apply to all tools.
     * If empty, the tool name is inferred from the method name by removing "post" prefix.
     */
    String toolName() default "";

    /**
     * Execution order when multiple hooks exist for the same tool.
     * Lower values execute first.
     */
    int order() default 0;
}
