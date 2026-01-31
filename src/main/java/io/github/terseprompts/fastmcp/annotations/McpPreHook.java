package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a pre-hook that executes before a tool invocation.
 *
 * Pre-hooks are executed before the target tool method is called, allowing for:
 * - Input validation
 * - Logging and auditing
 * - Authentication/authorization checks
 * - Parameter modification
 *
 * Example:
 * <pre>
 * {@code
 * @McpPreHook(toolName = "search", order = 1)
 * public void validateSearch(Map<String, Object> args) {
 *     if (!args.containsKey("query")) {
 *         throw new IllegalArgumentException("Query is required");
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpPreHook {
    /**
     * The name of the tool this hook applies to.
     * Use "*" to apply to all tools.
     * If empty, the tool name is inferred from the method name by removing "pre" prefix.
     */
    String toolName() default "";

    /**
     * Execution order when multiple hooks exist for the same tool.
     * Lower values execute first.
     */
    int order() default 0;
}
