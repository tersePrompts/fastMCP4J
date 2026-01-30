package com.ultrathink.fastmcp.hook;

import java.util.Map;

/**
 * Result of a hook execution.
 * <p>
 * For PRE_TOOL_USE hooks: DENY, MODIFY, or ALLOW
 * For POST_TOOL_USE hooks: MODIFY or observe
 */
public class HookResult {

    public enum Status {
        /** Deny execution - for PRE_TOOL_USE */
        DENY,
        /** Modify and continue */
        MODIFY,
        /** Allow execution unchanged */
        ALLOW
    }

    private final Status status;
    private final String message;
    private final Map<String, Object> modifiedArguments;
    private final Object modifiedResult;

    private HookResult(Status status, String message,
                      Map<String, Object> modifiedArguments, Object modifiedResult) {
        this.status = status;
        this.message = message;
        this.modifiedArguments = modifiedArguments;
        this.modifiedResult = modifiedResult;
    }

    /** Create a DENY result with a message */
    public static HookResult deny(String message) {
        return new HookResult(Status.DENY, message, null, null);
    }

    /** Create a MODIFY result for arguments */
    public static HookResult modifyArguments(Map<String, Object> modifiedArguments) {
        return new HookResult(Status.MODIFY, null, modifiedArguments, null);
    }

    /** Create a MODIFY result for return value */
    public static HookResult modifyResult(Object modifiedResult) {
        return new HookResult(Status.MODIFY, null, null, modifiedResult);
    }

    /** Create an ALLOW result */
    public static HookResult allow() {
        return new HookResult(Status.ALLOW, null, null, null);
    }

    public Status status() { return status; }
    public String message() { return message; }
    public Map<String, Object> modifiedArguments() { return modifiedArguments; }
    public Object modifiedResult() { return modifiedResult; }
}
