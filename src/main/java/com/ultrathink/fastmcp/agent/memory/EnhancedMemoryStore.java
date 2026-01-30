package com.ultrathink.fastmcp.agent.memory;

import java.util.List;

/**
 * Enhanced memory store interface with typed memory support.
 * Extends the existing MemoryStore interface.
 */
public interface EnhancedMemoryStore {

    /**
     * Store a memory entry with type and scope
     */
    void store(String sessionId, MemoryType type, MemoryScope scope, MemoryEntry entry);

    /**
     * Retrieve memories by type and scope
     */
    List<MemoryEntry> retrieve(String sessionId, MemoryType type, MemoryScope scope, int limit);

    /**
     * Search memories by content query (semantic search)
     */
    List<MemoryEntry> search(String sessionId, String query, MemoryType type, int limit);

    /**
     * Evict memories based on policy
     */
    void evict(String sessionId, EvictionPolicy policy);

    /**
     * Eviction policies
     */
    enum EvictionPolicy {
        LRU,     // Least recently used
        FIFO,    // First in first out
        IMPORTANCE, // Lowest importance first
        TIME_BASED  // Oldest entries first
    }
}
