package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark a method as an MCP tool.
 * <p>
 * Tools become callable by AI. Just annotate and return a value.
 * <p>
 * Example:
 * <pre>{@code
 * @McpTool(description = "Add two numbers")
 * public int add(int a, int b) {
 *     return a + b;
 * }
 * }</pre>
 * <p>
 * <b>Make it async — just add {@link McpAsync} and return {@code Mono<?>}, that's it!</b>
 * <pre>{@code
 * @McpTool(description = "Process data")
 * @McpAsync  // ← One annotation, that's it!
 * public Mono<String> process(String input) {
 *     return Mono.fromCallable(() -> slowOperation(input))
 *         .subscribeOn(Schedulers.boundedElastic());
 * }
 * }</pre>
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface McpTool {
    /** Tool name. Defaults to method name if empty. */
    String name() default "";

    /** What this tool does — shown to AI. */
    String description() default "";

    /**
     * Icons for the tool.
     * Format: "src" or "src:mimeType:sizes:theme"
     * Multiple icons can be provided.
     */
    String[] icons() default {};
}
