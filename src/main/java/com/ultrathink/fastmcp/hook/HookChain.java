package com.ultrathink.fastmcp.hook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Chain of hooks for a specific hook type.
 * Executes hooks in priority order and handles DENY/MODIFY responses.
 */
@Slf4j
public class HookChain {

    private final HookType hookType;
    private final List<RegisteredHook> hooks = new CopyOnWriteArrayList<>();

    public HookChain(HookType hookType) {
        this.hookType = hookType;
    }

    /**
     * Register a hook.
     */
    public void registerHook(RegisteredHook hook) {
        hooks.add(hook);
        // Sort by priority (higher priority first)
        hooks.sort(Comparator.comparingInt(RegisteredHook::priority).reversed());
    }

    /**
     * Execute PRE_TOOL_USE hooks.
     * Returns null if allowed, or modified arguments if modified.
     * @throws HookDeniedException if any hook denies execution
     */
    public Map<String, Object> executePreToolUse(String toolName, Map<String, Object> arguments) {
        for (RegisteredHook hook : hooks) {
            if (hook.type() != HookType.PRE_TOOL_USE) continue;

            HookResult result = hook.executor().execute(toolName, arguments);
            switch (result.status()) {
                case DENY:
                    throw new HookDeniedException(
                        "Tool '" + toolName + "' denied by hook: " + result.message()
                    );
                case MODIFY:
                    arguments = result.modifiedArguments();
                    log.debug("Hook modified arguments for tool '{}'", toolName);
                    break;
                case ALLOW:
                    break;
            }
        }
        return arguments;
    }

    /**
     * Execute POST_TOOL_USE hooks.
     * Returns the final result after all modifications.
     */
    public Object executePostToolUse(String toolName, Map<String, Object> arguments, Object result) {
        for (RegisteredHook hook : hooks) {
            if (hook.type() != HookType.POST_TOOL_USE) continue;

            HookResult hookResult = hook.executor().execute(toolName, arguments, result);
            if (hookResult.status() == HookResult.Status.MODIFY && hookResult.modifiedResult() != null) {
                result = hookResult.modifiedResult();
            }
        }
        return result;
    }

    /**
     * Execute session lifecycle hooks.
     */
    public void executeSessionHook(String sessionId, Map<String, Object> context) {
        for (RegisteredHook hook : hooks) {
            if (!isSessionHook(hook.type())) continue;

            try {
                hook.executor().executeSessionHook(sessionId, context);
            } catch (Exception e) {
                log.error("Hook error during {} for session {}", hook.type(), sessionId, e);
            }
        }
    }

    private boolean isSessionHook(HookType type) {
        return type == HookType.SESSION_BOOTSTRAP
            || type == HookType.SESSION_START
            || type == HookType.SESSION_EXPIRING
            || type == HookType.SESSION_END;
    }

    /**
     * Get the number of registered hooks.
     */
    public int size() {
        return hooks.size();
    }

    /**
     * Check if any hooks are registered.
     */
    public boolean isEmpty() {
        return hooks.isEmpty();
    }

    /**
     * A registered hook with metadata.
     */
    public record RegisteredHook(
        HookType type,
        int priority,
        String pattern,
        HookExecutor executor
    ) {}
}
