package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpResource {
    String uri();
    String name() default "";
    String description() default "";
    String mimeType() default "text/plain";
    
    /**
     * Icons for the resource.
     * Format: "src" or "src:mimeType:sizes:theme"
     * Multiple icons can be provided.
     */
    String[] icons() default {};
}