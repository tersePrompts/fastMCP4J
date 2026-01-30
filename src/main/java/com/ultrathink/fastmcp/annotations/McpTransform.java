package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply a response transformer to a tool.
 * <p>
 * Transformers are applied in order (by order value) after the tool executes.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpTool
 * @McpTransform(value = "sanitizeHtml", order = 10)
 * public String fetchContent(String url) {
 *     return fetchRawContent(url);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTransform {

    /**
     * The name of the transformer to apply.
     * Can be a registered transformer name or a bean name.
     */
    String value();

    /**
     * The execution order (lower values execute first).
     */
    int order() default 100;

    /**
     * Whether to apply the transformer asynchronously.
     */
    boolean async() default false;
}
