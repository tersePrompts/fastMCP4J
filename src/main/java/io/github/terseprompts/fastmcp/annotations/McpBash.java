package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables the <b>bash</b> tool on a server class.
 *
 * <p>Two modes (see {@link BashMode}):
 *
 * <ul>
 *   <li><b>{@link BashMode#SANDBOX} (default)</b> — scripts run in a
 *       bashkit4j in-memory sandbox: a virtual computer with its own
 *       filesystem, processes, and environment. Nothing on the host can be
 *       read, written, or executed; the only way in is directories you
 *       explicitly {@link #mounts() mount}, allowlisted by
 *       {@link #allowMountsUnder()} and enforced inside the native library
 *       (mount roots are canonicalized, so {@code ..} segments and symlink
 *       tricks can't escape). Requires the optional
 *       {@code io.github.terseprompts:bashkit4j} dependency.</li>
 *   <li><b>{@link BashMode#HOST}</b> — legacy mode: commands execute in the
 *       real shell (cmd.exe / bash / zsh) with path guardrails and timeout.
 *       ⚠️ The process has full OS access — only for trusted environments.</li>
 * </ul>
 *
 * <p>State persists across tool calls in sandbox mode (cwd, environment
 * variables, files), enabling multi-step agent workflows.
 *
 * <p>Example — safe default:
 * <pre>{@code
 * @McpServer(name = "demo", version = "1.0")
 * @McpBash   // sandbox mode, no host access
 * public class Demo { }
 * }</pre>
 *
 * <p>Example — read-only access to a project directory:
 * <pre>{@code
 * @McpBash(
 *     mode = BashMode.SANDBOX,
 *     allowMountsUnder = "C:/dev/projects",
 *     mounts = "/project=C:/dev/projects/my-app"   // read-only; append ":rw" for writable
 * )
 * }</pre>
 *
 * <p>Example — trusted local automation on the real shell:
 * <pre>{@code
 * @McpBash(
 *     mode = BashMode.HOST,
 *     timeout = 30,
 *     visibleAfterBasePath = "/home/user/projects/*",
 *     notAllowedPaths = {"/etc", "/root"}
 * )
 * }</pre>
 *
 * @see BashMode
 * @see io.github.terseprompts.fastmcp.mcptools.bash.BashTool
 * @see io.github.terseprompts.fastmcp.mcptools.bash.SandboxBashTool
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpBash {

    /** Execution mode. Default {@link BashMode#SANDBOX}. */
    BashMode mode() default BashMode.SANDBOX;

    /**
     * Command timeout in seconds, both modes. In sandbox mode a timed-out
     * script keeps running in the background until the sandbox's
     * {@code maxCommands} bound stops it; the next call gets a fresh sandbox.
     *
     * @return timeout in seconds, default 30
     */
    int timeout() default 30;

    // ------------------------------------------------------- sandbox mode

    /**
     * Sandbox mode: maximum number of commands a single script may execute
     * (bounds runaway loops/fork bombs inside the virtual machine).
     *
     * @return command limit, default 10 000
     */
    long maxCommands() default 10_000;

    /** Sandbox mode: user shown to scripts ({@code whoami}), default "agent". */
    String username() default "agent";

    /** Sandbox mode: hostname shown to scripts, default "sandbox". */
    String hostname() default "sandbox";

    /** Sandbox mode: working directory at first use, default "/". */
    String cwd() default "/";

    /**
     * Sandbox mode: seed environment variables, {@code "KEY=VALUE"} strings.
     *
     * @return env entries, default none
     */
    String[] env() default {};

    /**
     * Sandbox mode: host directories to mount into the virtual filesystem.
     * Format {@code "/vfs/path=host/dir"} (read-only) or
     * {@code "/vfs/path=host/dir:rw"} (writable). Every host path must
     * resolve under one of the {@link #allowMountsUnder()} prefixes — enforced
     * inside the native library.
     *
     * @return mount specs, default none
     */
    String[] mounts() default {};

    /**
     * Sandbox mode: host path prefixes mounts may resolve under. No prefixes
     * → no mounts, ever; the sandbox stays airtight.
     *
     * @return allowed mount prefixes, default none
     */
    String[] allowMountsUnder() default {};

    // ---------------------------------------------------------- host mode

    /**
     * Host mode: whitelist pattern for allowed working directories
     * (e.g. {@code /home/user/projects/*}). Empty = any directory (NOT
     * RECOMMENDED for host mode).
     *
     * @return path pattern, default empty
     */
    String visibleAfterBasePath() default "";

    /**
     * Host mode: blacklist of host paths where commands are never allowed
     * (e.g. {@code {"/etc", "/root", "/home/user/.ssh"}}).
     *
     * @return blocked paths, default none
     */
    String[] notAllowedPaths() default {};
}
