package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ⚠️⚠️⚠️ EXTREME SECURITY WARNING ⚠️⚠️⚠️
 * <p>
 * <b>THIS ANNOTATION ENABLES ARBITRARY SHELL COMMAND EXECUTION ON YOUR HOST SYSTEM.</b>
 * <p>
 * <b>DO NOT USE IN PRODUCTION UNLESS:</b>
 * <ul>
 *   <li>Running on an isolated/honeypot server with NO internet access</li>
 *   <li>The AI/client is properly sandboxed (Docker, VM, air-gapped)</li>
 *   <li>You have implemented strict command whitelisting</li>
 *   <li>You understand and accept FULL responsibility for any damage</li>
 * </ul>
 * <p>
 * <b>RISKS OF USING THIS ANNOTATION:</b>
 * <ul>
 *   <li>🔥 <b>COMPLETE SYSTEM COMPROMISE</b> - Attacker can read/write/delete ANY file</li>
 *   <li>🔥 <b>ARBITRARY CODE EXECUTION</b> - Any program/script can be run</li>
 *   <li>🔥 <b>NETWORK EXFILTRATION</b> - Data can be sent to external servers</li>
 *   <li>🔥 <b>PERSISTENCE</b> - Backdoors, malware, keyloggers can be installed</li>
 *   <li>🔥 <b>LATERAL MOVEMENT</b> - Attack other systems on the network</li>
 * </ul>
 * <p>
 * <b>USE AT YOUR OWN RISK. THE AUTHORS ARE NOT RESPONSIBLE FOR ANY DAMAGE CAUSED.</b>
 * <p>
 * <b>Built-in Guardrails:</b>
 * <ul>
 *   <li>Directory validation - commands rejected if outside allowed paths</li>
 *   <li>Path blacklist - sensitive paths (/etc, /sys, /proc, etc.) blocked</li>
 *   <li>Change directory (cd) commands are validated</li>
 *   <li>Timeout protection - commands killed after configured timeout</li>
 * </ul>
 * <p>
 * When placed on a server class, this enables the following tool:
 * <ul>
 *   <li><b>bash</b> - Execute shell commands with OS-aware shell selection</li>
 * </ul>
 * <p>
 * The tool automatically detects the operating system and uses the appropriate shell:
 * <ul>
 *   <li>Windows: cmd.exe</li>
 *   <li>macOS: /bin/zsh</li>
 *   <li>Linux: /bin/bash</li>
 * </ul>
 * <p>
 * <b>RECOMMENDED SETTINGS FOR SANDBOXED ENVIRONMENTS:</b>
 * <pre>
 * {@code
 * @McpBash(
 *     timeout = 30,
 *     visibleAfterBasePath = "/sandbox/allowed/*",   // Restrict to safe directory
 *     notAllowedPaths = {"/etc", "/root", "/home"}    // Block system paths
 * )
 * }
 * </pre>
 *
 * @see io.github.terseprompts.fastmcp.mcptools.bash.BashTool
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpBash {
    /**
     * Command timeout in seconds.
     * Commands will be killed after this duration to prevent hanging/infinite loops.
     * <p>
     * Lower values reduce the window for malicious activity. Recommended: 10-60 seconds.
     *
     * @return timeout in seconds, default 30
     */
    int timeout() default 30;

    /**
     * Whitelist pattern for allowed working directories.
     * Commands will ONLY execute if current directory matches this pattern.
     * <p>
     * Supports wildcards: <code>/home/user/projects/*</code>, <code>C:\sandbox\*</code>
     * <p>
     * Leave empty to allow any directory (NOT RECOMMENDED).
     *
     * @return path pattern, default empty (any directory)
     */
    String visibleAfterBasePath() default "";

    /**
     * Blacklist of paths where commands are NEVER allowed.
     * Commands attempting to access these paths will be rejected immediately.
     * <p>
     * <b>RECOMMENDED PATHS TO BLOCK:</b>
     * <ul>
     *   <li>Linux/macOS: /etc, /root, /sys, /proc, /boot, /home/[user]/.ssh</li>
     *   <li>Windows: C:\Windows\System32, C:\Users\[user]\.ssh</li>
     * </ul>
     *
     * @return array of blocked paths, default empty
     */
    String[] notAllowedPaths() default {};
}
