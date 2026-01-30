package com.ultrathink.fastmcp.agent.memory;

/**
 * Memory scopes defining visibility and lifecycle of memories.
 */
public enum MemoryScope {
    /**
     * Visible only within current session, cleared on session end
     */
    SESSION,

    /**
     * Visible to all sessions for a specific user
     */
    USER,

    /**
     * Visible to all users within a tenant
     */
    TENANT,

    /**
     * Visible across all tenants and users
     */
    GLOBAL
}
