package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method to be invoked once during graceful server shutdown.
 * <p>
 * The annotated method must take no arguments.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface McpOnShutdown {
}