package io.github.terseprompts.fastmcp.middleware;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MiddlewareChain}.
 */
class MiddlewareChainTest {

    private static final Map<String, Object> ARGS = Map.of("a", "1");

    private static final Middleware.Invocation CORE = (name, args) -> {
        assertEquals("tool", name);
        return "core-result(" + args.get("a") + ")";
    };

    @Test
    void testEmptyChainPassesThrough() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        assertTrue(chain.isEmpty());

        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("core-result(1)", result);
    }

    @Test
    void testMiddlewareTransformsArguments() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        chain.add((name, args, next) -> {
            Map<String, Object> transformed = new java.util.HashMap<>(args);
            transformed.put("a", "transformed");
            return next.invoke(name, transformed);
        });

        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("core-result(transformed)", result);
    }

    @Test
    void testMiddlewareTransformsResult() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        chain.add((name, args, next) -> {
            Object raw = next.invoke(name, args);
            return "wrapped[" + raw + "]";
        });

        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("wrapped[core-result(1)]", result);
    }

    @Test
    void testMultipleMiddlewareOrdering() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        chain.add((name, args, next) -> {
            Object raw = next.invoke(name, args);
            return "first(" + raw + ")";
        });
        chain.add((name, args, next) -> {
            Object raw = next.invoke(name, args);
            return "second(" + raw + ")";
        });

        // First-registered middleware wraps the outermost.
        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("first(second(core-result(1)))", result);
    }

    @Test
    void testMiddlewareShortCircuits() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        chain.add((name, args, next) -> "short-circuit");
        chain.add((name, args, next) -> {
            fail("Inner middleware must not run when outer short-circuits");
            return null;
        });

        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("short-circuit", result);
    }

    @Test
    void testAddNullIgnores() throws Exception {
        MiddlewareChain chain = new MiddlewareChain();
        chain.add(null);
        assertTrue(chain.isEmpty());
        assertEquals(0, chain.size());

        Object result = chain.intercept("tool", ARGS, CORE);
        assertEquals("core-result(1)", result);
    }

    @Test
    void testAddAllCombinesChains() throws Exception {
        MiddlewareChain first = new MiddlewareChain();
        first.add((name, args, next) -> {
            Object raw = next.invoke(name, args);
            return "A(" + raw + ")";
        });
        MiddlewareChain second = new MiddlewareChain();
        second.add((name, args, next) -> {
            Object raw = next.invoke(name, args);
            return "B(" + raw + ")";
        });

        first.addAll(second);
        Object result = first.intercept("tool", ARGS, CORE);
        assertEquals("A(B(core-result(1)))", result);
    }

    @Test
    void testMiddlewareSeesTransformedArgsAndOriginalResult() throws Exception {
        List<String> trace = new ArrayList<>();
        MiddlewareChain chain = new MiddlewareChain();
        chain.add((name, args, next) -> {
            trace.add("outer-args:" + args.get("a"));
            Map<String, Object> transformed = new java.util.HashMap<>(args);
            transformed.put("a", "x");
            Object raw = next.invoke(name, transformed);
            trace.add("outer-result:" + raw);
            return raw;
        });
        chain.add((name, args, next) -> {
            trace.add("inner-args:" + args.get("a"));
            Object raw = next.invoke(name, args);
            trace.add("inner-result:" + raw);
            return raw;
        });

        chain.intercept("tool", ARGS, CORE);

        assertEquals(List.of(
            "outer-args:1",
            "inner-args:x",
            "inner-result:core-result(x)",
            "outer-result:core-result(x)"
        ), trace);
    }
}