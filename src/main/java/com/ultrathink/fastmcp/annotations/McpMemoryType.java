package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ultrathink.fastmcp.agent.memory.MemoryType;
import com.ultrathink.fastmcp.agent.memory.MemoryScope;

/**
 * Mark a field or method for memory injection.
 * <p>
 * When used on a field, the framework injects an EnhancedMemoryStore instance
 * configured with the specified type and scope.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpServer
 * public class MyServer {
 *
 *     @McpMemoryType(type = MemoryType.EPISODIC, scope = MemoryScope.SESSION)
 *     private EnhancedMemoryStore sessionMemory;
 *
 *     @McpTool
 *     public String remember(String fact) {
 *         sessionMemory.write(fact, Map.of("type", "fact"));
 *         return "Remembered: " + fact;
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpMemoryType {

    /**
     * The type of memory to inject.
     */
    MemoryType type() default MemoryType.WORKING;

    /**
     * The scope of memory to inject.
     */
    MemoryScope scope() default MemoryScope.SESSION;
}
