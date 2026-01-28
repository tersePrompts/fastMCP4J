package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable bash/shell command execution tools for an MCP server.
 * <p>
 * <b>⚠️ SECURITY WARNING:</b> This tool allows execution of arbitrary shell commands on the host system.
 * This includes the ability to:
 * <ul>
 *   <li>Read, write, and delete any files the application has access to</li>
 *   <li>Execute any program or script installed on the system</li>
 *   <li>Make network requests to external services</li>
 *   <li>Modify system settings and configurations</li>
 * </ul>
 * <p>
 * <b>Use with caution:</b>
 * <ul>
 *   <li>Only enable in trusted environments where the AI/client is properly sandboxed</li>
 *   <li>Consider implementing command whitelisting for production use</li>
 *   <li>Be aware that commands run with the same permissions as the Java application</li>
 *   <li>The LLM may execute harmful commands if not properly instructed</li>
 * </ul>
 * <p>
 * When placed on a server class, this enables the following tools:
 * <ul>
 *   <li><b>execute_command</b> - Execute shell commands with OS-aware shell selection</li>
 * </ul>
 * <p>
 * The tool automatically detects the operating system and uses the appropriate shell:
 * <ul>
 *   <li>Windows: cmd.exe</li>
 *   <li>macOS: /bin/zsh</li>
 *   <li>Linux: /bin/bash</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpServer
 * @McpBash(timeout = 60)
 * public class MyServer {
 *     // Server implementation
 * }
 * }
 * </pre>
 *
 * @see com.ultrathink.fastmcp.mcptools.bash.BashTool
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpBash {
    /**
     * Optional custom timeout in seconds for command execution.
     * Default is 30 seconds.
     */
    int timeout() default 30;

    /**
     * Optional path restriction - commands will only execute if the current working directory
     * matches this pattern. Supports wildcards (e.g., "/U01/*", "/home/user/projects/*").
     * <p>
     * If specified, commands executed outside this path will be rejected.
     * </p>
     */
    String visibleAfterBasePath() default "";

    /**
     * Optional blacklist of paths where commands are never allowed.
     * Commands attempting to access these paths will be rejected.
     * <p>
     * Examples: {"/etc", "/sys", "/proc", "C:\\Windows\\System32"}
     * </p>
     */
    String[] notAllowedPaths() default {};
}
