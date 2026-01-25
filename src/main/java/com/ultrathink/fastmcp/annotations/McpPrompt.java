package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpPrompt {
    String name() default "";
    String description() default "";
    
    /**
     * Icons for the prompt.
     * Format: "src" or "src:mimeType:sizes:theme"
     * Multiple icons can be provided.
     */
    String[] icons() default {};
}