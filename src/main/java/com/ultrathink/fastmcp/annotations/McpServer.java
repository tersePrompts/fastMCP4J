package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface McpServer {
    String name();
    String version() default "1.0.0";
    String instructions() default "";
    
    /**
     * Icons for the server implementation.
     * Format: "src" or "src:mimeType:sizes:theme"
     * Multiple icons can be provided.
     * 
     * Examples:
     * - "https://example.com/icon.png"
     * - "https://example.com/icon.png:image/png:48x48:light"
     * - "data:image/svg+xml;base64,PHN2Zy...:any"
     */
    String[] icons() default {};
}