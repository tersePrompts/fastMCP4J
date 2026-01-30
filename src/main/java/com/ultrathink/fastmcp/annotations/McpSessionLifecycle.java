package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called during session lifecycle events.
 * <p>
 * The method signature should be:
 * <pre>
 * {@code
 * public void onSessionStart(String sessionId)
 * public void onSessionEnd(String sessionId)
 * }
 * </pre>
 * <p>
 * Supported events:
 * <ul>
 *   <li>BOOTSTRAP - When a session is being bootstrapped</li>
 *   <li>START - When a session becomes active</li>
 *   <li>EXPIRING - When a session is about to expire</li>
 *   <li>END - When a session is terminated</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpServer
 * public class MyServer {
 *
 *     @McpSessionLifecycle(Event.START)
 *     public void onSessionStart(String sessionId) {
 *         // Initialize session resources
 *     }
 *
 *     @McpSessionLifecycle(Event.END)
 *     public void onSessionEnd(String sessionId) {
 *         // Clean up session resources
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpSessionLifecycle {

    /**
     * The lifecycle event this method should be called for.
     */
    Event value();

    /**
     * Lifecycle events.
     */
    enum Event {
        /**
         * Called when a session is being bootstrapped.
         * Method signature: void onBootstrap(String sessionId, Map<String, Object> config)
         */
        BOOTSTRAP,

        /**
         * Called when a session becomes active.
         * Method signature: void onStart(String sessionId)
         */
        START,

        /**
         * Called when a session is about to expire.
         * Method signature: void onExpiring(String sessionId)
         */
        EXPIRING,

        /**
         * Called when a session ends.
         * Method signature: void onEnd(String sessionId, String reason)
         */
        END
    }
}
