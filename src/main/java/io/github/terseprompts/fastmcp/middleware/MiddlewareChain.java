package io.github.terseprompts.fastmcp.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ordered chain of {@link Middleware} instances applied around a core tool
 * invocation. Middleware registered first runs first (outermost).
 */
public class MiddlewareChain {

    private final List<Middleware> middleware = new ArrayList<>();

    /**
     * Append a middleware to the end of the chain.
     */
    public MiddlewareChain add(Middleware m) {
        if (m != null) {
            middleware.add(m);
        }
        return this;
    }

    /**
     * Append all middleware from another chain, preserving their order.
     */
    public MiddlewareChain addAll(MiddlewareChain other) {
        if (other != null) {
            middleware.addAll(other.middleware);
        }
        return this;
    }

    /**
     * @return true when no middleware is registered
     */
    public boolean isEmpty() {
        return middleware.isEmpty();
    }

    /**
     * Run all middleware around the given core invocation and return the final result.
     * When the chain is empty the core invocation is called directly.
     */
    public Object intercept(String toolName, Map<String, Object> arguments, Middleware.Invocation core) throws Exception {
        Middleware.Invocation invocation = core;
        for (int i = middleware.size() - 1; i >= 0; i--) {
            Middleware m = middleware.get(i);
            Middleware.Invocation next = invocation;
            invocation = (name, args) -> m.intercept(name, args, next);
        }
        return invocation.invoke(toolName, arguments);
    }

    /**
     * Number of registered middleware entries.
     */
    public int size() {
        return middleware.size();
    }
}