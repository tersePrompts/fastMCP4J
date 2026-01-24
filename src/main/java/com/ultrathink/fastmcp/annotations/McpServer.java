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
}