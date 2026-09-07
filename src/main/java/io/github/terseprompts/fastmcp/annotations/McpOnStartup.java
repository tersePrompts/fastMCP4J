package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method to be invoked once at server startup, after the server class
 * instance has been created and dependency injection has run, but before tool,
 * resource and prompt registration.
 * <p>
 * The annotated method may take no arguments, or a single {@code Context} (or
 * {@code @McpContext}) parameter to receive the current context if available.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface McpOnStartup {
}