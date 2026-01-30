package com.ultrathink.fastmcp.agent.memory;

/**
 * Memory types for categorizing stored information.
 */
public enum MemoryType {
    /**
     * Time-ordered events and experiences (conversation history, actions taken)
     */
    EPISODIC,

    /**
     * Knowledge graph and facts (entities, relationships, concepts)
     */
    SEMANTIC,

    /**
     * Skills and how-to knowledge (procedures, recipes, algorithms)
     */
    PROCEDURAL,

    /**
     * Short-term task context (current task, working variables, temporary state)
     */
    WORKING
}
