package io.github.terseprompts.fastmcp.annotations;

/**
 * Execution mode for the {@link McpBash} bash tool.
 *
 * @see McpBash
 */
public enum BashMode {
    /**
     * Run scripts inside a bashkit4j in-memory sandbox: a virtual computer with
     * its own filesystem, processes, and environment. The host is unreachable
     * by construction — the only bridge is directories explicitly mounted via
     * {@link McpBash#mounts()}, allowlisted by {@link McpBash#allowMountsUnder()}
     * and enforced inside the native library.
     * <p>
     * Requires the optional {@code io.github.terseprompts:bashkit4j} dependency.
     */
    SANDBOX,

    /**
     * Run commands in the real host shell (cmd.exe / bash / zsh) with the
     * legacy guardrails ({@link McpBash#visibleAfterBasePath()},
     * {@link McpBash#notAllowedPaths()}, timeout). Only for trusted
     * environments — the process has full OS access.
     */
    HOST
}
