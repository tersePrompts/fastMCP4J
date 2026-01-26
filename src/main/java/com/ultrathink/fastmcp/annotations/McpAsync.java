package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Make a tool async — just add this annotation, that's it!
 * <p>
 * Return {@code Mono<?><?>} from your method and FastMCP4J handles the rest.
 * Uses Project Reactor for reactive streams.
 * <p>
 * <b>Sync vs Async — one annotation difference:</b>
 * <pre>{@code
 * // Sync tool
 * @McpTool(description = "Process data")
 * public String process(String input) {
 *     return slowOperation(input);
 * }
 *
 * // Async tool — just add @McpAsync and return Mono<?>
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
public @interface McpAsync {
}
