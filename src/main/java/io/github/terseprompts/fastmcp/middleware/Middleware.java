package io.github.terseprompts.fastmcp.middleware;

import java.util.Map;

/**
 * Middleware wraps a tool invocation, allowing request/response transforms
 * and short-circuiting.
 *
 * <p>A middleware runs at the outermost layer of a tool call: it receives the
 * original arguments, may transform them before calling {@link Invocation#invoke},
 * and may transform the returned result afterwards.
 */
@FunctionalInterface
public interface Middleware {

    /**
     * Intercept a tool invocation.
     *
     * @param toolName  the name of the tool being invoked
     * @param arguments the (possibly already transformed) arguments
     * @param next      the next invocation in the chain
     * @return the result of the invocation, possibly transformed
     * @throws Exception if the invocation or middleware fails
     */
    Object intercept(String toolName, Map<String, Object> arguments, Invocation next) throws Exception;

    /**
     * The continuation of a middleware chain.
     */
    @FunctionalInterface
    interface Invocation {
        /**
         * Invoke the next handler in the chain (or the core tool invocation).
         *
         * @param toolName  the name of the tool being invoked
         * @param arguments the arguments to invoke with
         * @return the raw result (for async tools this is a {@code Mono})
         * @throws Exception if the underlying tool invocation fails
         */
        Object invoke(String toolName, Map<String, Object> arguments) throws Exception;
    }
}