package io.github.terseprompts.fastmcp.mcptools.bash;

import io.github.terseprompts.Bash;
import io.github.terseprompts.BashException;
import io.github.terseprompts.ExecResult;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Bash tool backed by a bashkit4j in-memory sandbox (the default
 * {@link io.github.terseprompts.fastmcp.annotations.BashMode#SANDBOX} mode of
 * {@link io.github.terseprompts.fastmcp.annotations.McpBash}).
 *
 * <p>Every script runs inside a virtual computer: private filesystem,
 * processes, and environment. The host machine is unreachable by construction
 * — the only bridge is directories explicitly mounted at construction, each
 * required to resolve under the configured {@code allowMountsUnder} prefixes
 * (enforced inside the native library, canonicalized, symlink-safe).
 *
 * <p>One sandbox instance serves all tool calls, so cwd, environment, and
 * files persist between calls. A script that outlives its timeout retires the
 * sandbox: the caller gets {@code TIMEDOUT} and the next call starts fresh.
 *
 * <p><b>Threading:</b> {@code Bash.exec} is internally synchronized, so
 * concurrent tool calls execute one at a time.
 */
public final class SandboxBashTool implements BashExecutor {

    private static final int MAX_SCRIPT_LENGTH = 50_000;

    private final int defaultTimeoutSeconds;
    private final String username;
    private final String hostname;

    private final Object lock = new Object();
    private final java.util.function.Supplier<Bash> sandboxFactory;
    private Bash sandbox;
    private boolean closed;

    private final ExecutorService execPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "fastmcp-bash-sandbox");
        t.setDaemon(true);
        return t;
    });

    /**
     * Create the tool and its persistent sandbox. Configuration errors (bad
     * mount specs, mounts without an {@code allowMountsUnder} prefix) fail
     * fast here, at server startup.
     *
     * @throws IllegalStateException if bashkit4j is not on the classpath
     * @throws IllegalArgumentException on invalid mount/env specs
     */
    public SandboxBashTool(int timeoutSeconds, long maxCommands, String username, String hostname,
                           String cwd, String[] env, String[] mounts, String[] allowMountsUnder) {
        this.defaultTimeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        this.username = username == null || username.isBlank() ? "agent" : username;
        this.hostname = hostname == null || hostname.isBlank() ? "sandbox" : hostname;

        Bash.Builder builder = Bash.builder()
                .cwd(cwd == null || cwd.isBlank() ? "/" : cwd)
                .username(this.username)
                .hostname(this.hostname)
                .maxCommands(maxCommands > 0 ? maxCommands : 10_000);
        if (env != null) {
            for (String entry : env) {
                int eq = entry.indexOf('=');
                if (eq <= 0) {
                    throw new IllegalArgumentException(
                            "McpBash env entries must be KEY=VALUE, got: '" + entry + "'");
                }
                builder.env(entry.substring(0, eq), entry.substring(eq + 1));
            }
        }
        List<String> mountDescriptions = new ArrayList<>();
        if (allowMountsUnder != null && allowMountsUnder.length > 0) {
            builder.allowMountsUnder(allowMountsUnder);
        }
        if (mounts != null) {
            for (String spec : mounts) {
                Mount m = parseMount(spec);
                builder.mount(m.vfsPath, m.hostPath, m.writable);
                mountDescriptions.add(m.vfsPath + " → " + m.hostPath + (m.writable ? " (read-write)" : " (read-only)"));
            }
        }
        this.mountDescriptions = mountDescriptions;

        // Builder.build() only reads its config, so the same builder can mint
        // replacement sandboxes after a timeout retires the current one.
        this.sandboxFactory = builder::build;

        try {
            // Build eagerly so native/config problems surface at startup, not on first call.
            this.sandbox = sandboxFactory.get();
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException(
                    "bashkit4j is not on the classpath — add io.github.terseprompts:bashkit4j:0.2.0 "
                            + "to use @McpBash (sandbox mode)", e);
        }
    }

    private record Mount(String vfsPath, String hostPath, boolean writable) {}

    private static Mount parseMount(String spec) {
        int eq = spec.indexOf('=');
        if (eq <= 0 || eq == spec.length() - 1) {
            throw new IllegalArgumentException(
                    "McpBash mounts must be /vfs/path=host/dir[:rw], got: '" + spec + "'");
        }
        String vfsPath = spec.substring(0, eq);
        String rest = spec.substring(eq + 1);
        boolean writable = false;
        if (rest.toLowerCase().endsWith(":rw")) {
            writable = true;
            rest = rest.substring(0, rest.length() - 3);
        }
        if (!vfsPath.startsWith("/") || rest.isBlank()) {
            throw new IllegalArgumentException(
                    "McpBash mounts must be /vfs/path=host/dir[:rw], got: '" + spec + "'");
        }
        return new Mount(vfsPath, rest, writable);
    }

    private final List<String> mountDescriptions;

    @Override
    public BashResult executeCommand(String script) {
        return executeCommand(script, defaultTimeoutSeconds);
    }

    @Override
    public BashResult executeCommand(String script, int timeoutSeconds) {
        if (script == null || script.trim().isEmpty()) {
            return new BashResult(-1, "", "🚫 COMMAND BLOCKED: empty script not allowed", false, script);
        }
        if (script.length() > MAX_SCRIPT_LENGTH) {
            return new BashResult(-1, "", "🚫 COMMAND BLOCKED: script exceeds maximum length of "
                    + MAX_SCRIPT_LENGTH, false, script);
        }

        Bash s = ensureSandbox();
        Future<ExecResult> future = execPool.submit(() -> {
            try {
                return s.exec(script);
            } finally {
                if (s != sandbox) {
                    s.close(); // sandbox was retired while this script ran — free it
                }
            }
        });

        try {
            return map(future.get(timeoutSeconds, TimeUnit.SECONDS), script);
        } catch (TimeoutException e) {
            retire(s);
            return new BashResult(-1, "",
                    "Script timed out after " + timeoutSeconds + " seconds (sandbox reset for next call)",
                    true, script);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BashException be) {
                return new BashResult(-1, "", "Sandbox error: " + be.getMessage(), false, script);
            }
            return new BashResult(-1, "", "Sandbox error: " + cause.getMessage(), false, script);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BashResult(-1, "", "Interrupted", false, script);
        }
    }

    /**
     * Read a file from the sandbox filesystem (Java-side counterpart to
     * scripts writing output files).
     *
     * @throws io.github.terseprompts.BashException if the path doesn't exist
     */
    public String readSandboxFile(String path) {
        return new String(readSandboxFileBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Byte-level variant of {@link #readSandboxFile(String)}. */
    public byte[] readSandboxFileBytes(String path) {
        return ensureSandbox().readFileBytes(path);
    }

    private Bash ensureSandbox() {
        synchronized (lock) {
            if (closed) throw new IllegalStateException("SandboxBashTool closed");
            if (sandbox == null) {
                sandbox = sandboxFactory.get(); // constructor already validated the config
            }
            return sandbox;
        }
    }

    private void retire(Bash s) {
        synchronized (lock) {
            if (sandbox == s) {
                sandbox = null; // next call gets a fresh sandbox
            }
        }
    }

    private static BashResult map(ExecResult r, String script) {
        String stdout = r.stdout();
        if (r.stdoutTruncated()) {
            stdout += "\n[stdout truncated]";
        }
        String stderr = r.stderr() != null ? r.stderr() : "";
        if (r.stderrTruncated()) {
            stderr += "\n[stderr truncated]";
        }
        return new BashResult(r.exitCode(), stdout, stderr, false, script);
    }

    @Override
    public String getToolDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("🖥️ Sandboxed bash — an in-memory virtual computer.\n\n")
                .append("Scripts run against a private filesystem inside a sandbox; the host ")
                .append("machine is not reachable. Standard bash features (pipes, redirection, ")
                .append("command substitution) work. State persists between calls: cwd, ")
                .append("environment variables, and files.\n\n")
                .append("User: ").append(username).append("@").append(hostname)
                .append(" | Per-script command limit: ").append("configured")
                .append(" | Timeout: ").append(defaultTimeoutSeconds).append("s per call");
        if (!mountDescriptions.isEmpty()) {
            desc.append("\n\n📂 Mounted host directories (the only host access):\n");
            for (String m : mountDescriptions) {
                desc.append("  - ").append(m).append('\n');
            }
        } else {
            desc.append("\n\n📂 No host directories mounted — fully isolated.");
        }
        return desc.toString();
    }

    @Override
    public String getToolSchema() {
        return """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "The bash script to run in the sandbox. May be multi-line and use full bash syntax (pipes, redirection, command substitution)."
                },
                "timeout": {
                  "type": "integer",
                  "description": "Optional timeout in seconds (default: 30)."
                }
              },
              "required": ["command"]
            }
            """;
    }

    @Override
    public Mono<McpSchema.CallToolResult> handleToolCall(McpAsyncServerExchange exchange, Map<String, Object> arguments) {
        String script = (String) arguments.get("command");
        int timeout = arguments.get("timeout") != null
                ? Integer.parseInt(arguments.get("timeout").toString())
                : defaultTimeoutSeconds;

        BashResult result = executeCommand(script, timeout);

        String status = switch (result.getExitCode()) {
            case 0 -> "SUCCESS";
            case -1 -> result.isTimedOut() ? "TIMEOUT" : "FAILED";
            default -> "FAILED";
        };
        String textOutput = String.format(
                "Exit Code: %d | Status: %s\n\n%s",
                result.getExitCode(), status, result.getOutput());
        if (result.getStderr() != null && !result.getStderr().isEmpty()) {
            textOutput += "\nStderr:\n" + result.getStderr();
        }

        return Mono.just(McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(textOutput)))
                .build());
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            Bash s = sandbox;
            sandbox = null;
            if (s != null) {
                s.close();
            }
        }
        execPool.shutdownNow();
    }
}
