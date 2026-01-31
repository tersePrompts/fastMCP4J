package io.github.terseprompts.fastmcp.annotations;

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
     * Additional tool classes to scan for @McpTool, @McpResource, @McpPrompt annotations.
     * This allows organizing tools across multiple classes instead of everything in one class.
     * <p>
     * Example:
     * <pre>
     * &#64;McpServer(modules = {UserTools.class, AdminTools.class})
     * </pre>
     */
    Class<?>[] modules() default {};

    /**
     * Base package to scan for @McpTool, @McpResource, @McpPrompt annotations.
     * Scans all classes in the package and sub-packages automatically.
     * <p>
     * Example:
     * <pre>
     * &#64;McpServer(scanBasePackage = "com.myapp.tools")
     * </pre>
     */
    String scanBasePackage() default "";

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