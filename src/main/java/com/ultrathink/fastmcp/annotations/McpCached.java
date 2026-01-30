package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Mark a tool method to cache its results.
 * <p>
 * Cached results are returned for identical arguments within the TTL period.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpTool
 * @McpCached(ttl = 60, ttlUnit = TimeUnit.SECONDS)
 * public String expensiveOperation(String input) {
 *     // Expensive computation
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpCached {

    /**
     * Time-to-live for cached results.
     */
    long ttl() default 300;

    /**
     * Time unit for TTL.
     */
    TimeUnit ttlUnit() default TimeUnit.SECONDS;

    /**
     * Maximum number of cached entries.
     * 0 means no limit (subject to global cache limits).
     */
    int maxSize() default 0;

    /**
     * Include arguments in cache key.
     * If false, only the tool name is used (single result for all calls).
     */
    boolean includeArguments() default true;

    /**
     * Cache key prefix - useful for tools with same arguments but different contexts.
     */
    String keyPrefix() default "";
}
