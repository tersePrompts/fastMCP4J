package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field to be populated by dependency injection after the server class
 * instance is constructed.
 * <p>
 * The value is resolved from the server's {@code InjectionRegistry}: first by
 * field name matching a registered bean id, then by assignable type matching a
 * registered bean type. When no matching bean is registered the field is left
 * unchanged (null).
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface McpInject {
}