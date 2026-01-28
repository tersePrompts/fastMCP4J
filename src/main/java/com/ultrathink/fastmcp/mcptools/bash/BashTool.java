package com.ultrathink.fastmcp.mcptools.bash;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bash/shell command execution tool for MCP.
 * Supports OS-aware command execution with Windows (cmd.exe), Linux (bash), and macOS (zsh).
 * The tool description dynamically includes the detected OS and shell information.
 * <p>
 * <b>⚠️ SECURITY WARNING:</b> This tool allows arbitrary command execution which can:
 * <ul>
 *   <li>Read, write, or delete files on the host system</li>
 *   <li>Execute any program or script</li>
 *   <li>Make network requests to external services</li>
 *   <li>Modify system configurations</li>
 * </ul>
 * <p>
 * Only use in trusted environments with proper sandboxing.
 * <p>
 * Path restrictions can be configured via constructor:
 * <ul>
 *   <li><code>visibleAfterBasePath</code> - only allow commands in this path pattern (e.g., "/home/user/projects/*")</li>
 *   <li><code>notAllowedPaths</code> - blacklist of paths never allowed (e.g., {"/etc", "/sys", "/proc"})</li>
 * </ul>
 */
public class BashTool {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private final int timeoutSeconds;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final String osDescription;
    private final String visibleAfterBasePath;
    private final List<String> notAllowedPaths;

    public BashTool() {
        this(DEFAULT_TIMEOUT_SECONDS, "", List.of());
    }

    public BashTool(int timeoutSeconds) {
        this(timeoutSeconds, "", List.of());
    }

    public BashTool(int timeoutSeconds, String visibleAfterBasePath, List<String> notAllowedPaths) {
        this.timeoutSeconds = timeoutSeconds;
        this.visibleAfterBasePath = visibleAfterBasePath;
        this.notAllowedPaths = notAllowedPaths != null ? notAllowedPaths : List.of();
        this.osDescription = buildOsDescription();
    }

    /**
     * Build a description string that includes OS and shell information.
     * This will be embedded in the tool description so the LLM knows the platform.
     */
    private static String buildOsDescription() {
        OsType osType = OsDetector.getOsType();
        String shell = OsDetector.getShellName();
        String osName = System.getProperty("os.name");

        return String.format("Platform: %s | Shell: %s | OS Type: %s",
            osName, shell, osType.name());
    }

    /**
     * Get the tool description with embedded OS information.
     * This is called when registering the tool so the description is dynamic.
     */
    public String getToolDescription() {
        OsType osType = OsDetector.getOsType();
        String toolName = getToolName(osType);
        String cautionWarning = getCautionWarning(osType);

        StringBuilder desc = new StringBuilder();
        desc.append(String.format(
            "⚠️ %s\n\n" +
            "A MCP bash tool for %s.\n\n" +
            "Execute shell commands on the current system using the detected shell. " +
            "Use appropriate commands for the platform.\n\n" +
            "Platform: %s | Shell: %s | OS Type: %s",
            cautionWarning, toolName, System.getProperty("os.name"),
            OsDetector.getShellName(), osType.name()
        ));

        if (!visibleAfterBasePath.isEmpty()) {
            desc.append("\n\n⚠️ Path Restriction: Commands only allowed in: ").append(visibleAfterBasePath);
        }
        if (!notAllowedPaths.isEmpty()) {
            desc.append("\n\n⚠️ Blocked Paths: ").append(String.join(", ", notAllowedPaths));
        }

        return desc.toString();
    }

    private static String getToolName(OsType osType) {
        return switch (osType) {
            case WINDOWS -> "Windows Command Prompt";
            case MACOS -> "macOS Zsh";
            case LINUX -> "Linux Bash";
            default -> "Unix Shell";
        };
    }

    private static String getCautionWarning(OsType osType) {
        return switch (osType) {
            case WINDOWS ->
                "SECURITY WARNING: This tool executes arbitrary Windows commands. " +
                "Use with caution - it can read/write files, execute programs, modify registry, " +
                "and change system settings. Windows commands can have powerful system-wide effects.";
            case MACOS ->
                "SECURITY WARNING: This tool executes arbitrary macOS commands. " +
                "Use with caution - it can read/write files, execute programs, modify system preferences, " +
                "and access sensitive macOS subsystems. Unix commands on macOS can access all user data.";
            case LINUX ->
                "SECURITY WARNING: This tool executes arbitrary Linux commands. " +
                "Use with caution - it can read/write files, execute programs, modify system configurations, " +
                "and access sensitive /sys, /proc, and /dev filesystems. Linux commands have full system access.";
            default ->
                "SECURITY WARNING: This tool executes arbitrary shell commands. " +
                "Use with caution - it can read/write files, execute programs, and modify system settings.";
        };
    }

    /**
     * Get platform information as a map (for internal use).
     */
    public Map<String, String> getPlatformInfo() {
        return Map.of(
            "os", System.getProperty("os.name"),
            "osVersion", System.getProperty("os.version"),
            "osArch", System.getProperty("os.arch"),
            "osType", OsDetector.getOsType().name(),
            "defaultShell", OsDetector.getShellName(),
            "javaVersion", System.getProperty("java.version")
        );
    }

    /**
     * Validate that the current working directory and command are allowed.
     * @return Error message if validation fails, null if valid
     */
    private String validatePathRestrictions(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "🚫 COMMAND BLOCKED: empty command not allowed";
        }

        String cwd = System.getProperty("user.dir");
        String cwdNormalized = cwd.replace('\\', '/');

        // Check notAllowedPaths blacklist (handle both path separators)
        for (String blocked : notAllowedPaths) {
            String blockedNormalized = blocked.replace('\\', '/');
            // Check if cwd starts with blocked path OR command contains blocked path
            if (cwdNormalized.startsWith(blockedNormalized) ||
                cwdNormalized.equals(blockedNormalized) ||
                command.contains(blocked) ||
                command.toLowerCase().contains(blocked.toLowerCase())) {
                return String.format("🚫 COMMAND BLOCKED: path '%s' is not allowed", blocked);
            }
        }

        // Check visibleAfterBasePath restriction
        if (!visibleAfterBasePath.isEmpty()) {
            // Convert glob pattern to regex
            String pattern = visibleAfterBasePath
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");

            // Handle both forward and backslashes
            String cwdForPattern = cwdNormalized.replace("/", "[/\\\\]");
            String patternForPattern = pattern.replace("/", "[/\\\\]");

            if (!cwdForPattern.matches(patternForPattern)) {
                return String.format("🚫 COMMAND BLOCKED: current directory '%s' does not match allowed pattern '%s'",
                    cwd, visibleAfterBasePath);
            }
        }

        // Additional security: check for dangerous commands in sensitive paths
        String[] dangerousPatterns = {
            "rm -rf /", "rm -rf /*", "rm -rf ~", "mkfs", "dd if=", "chmod 000", "chown root",
            "wget ", "curl ", "nc -l", "netcat", "ssh ", "scp ", "rsync", "ncat"
        };

        String cmdLower = command.toLowerCase().trim();
        for (String dangerous : dangerousPatterns) {
            if (cmdLower.contains(dangerous)) {
                return String.format("🚫 COMMAND BLOCKED: dangerous command '%s' detected", dangerous);
            }
        }

        // Check for directory traversal attempts
        if (command.contains("../") || command.contains("..\\\\")) {
            return "🚫 COMMAND BLOCKED: directory traversal (../) not allowed";
        }

        return null; // Valid
    }

    /**
     * Additional validation for change directory commands.
     * Prevents directory traversal to sensitive areas.
     * @param targetDir The target directory for cd command
     * @return Error message if validation fails, null if valid
     */
    private String validateChangeDirectory(String targetDir) {
        if (targetDir == null || targetDir.isEmpty()) {
            return null;
        }

        // Normalize the path
        Path targetPath = Paths.get(targetDir).normalize();
        String targetPathStr = targetPath.toString();

        // Check for directory traversal
        if (targetPathStr.contains("..")) {
            return String.format("🚫 CD BLOCKED: directory traversal not allowed: '%s'", targetDir);
        }

        // Check against blocked paths
        for (String blocked : notAllowedPaths) {
            if (targetPathStr.startsWith(blocked)) {
                return String.format("🚫 CD BLOCKED: cannot cd to '%s' (blocked path)", targetDir);
            }
        }

        return null; // Valid
    }

    /**
     * Execute a shell command.
     * @param command The command to execute (e.g., "ls -la" or "dir")
     * @return BashResult with exit code, stdout, stderr
     */
    public BashResult executeCommand(String command) {
        return executeCommand(command, timeoutSeconds);
    }

    /**
     * Execute a shell command with custom timeout.
     * @param command The command to execute
     * @param timeoutSec Timeout in seconds
     * @return BashResult with exit code, stdout, stderr
     */
    public BashResult executeCommand(String command, int timeoutSec) {
        // Check path restrictions
        String pathError = validatePathRestrictions(command);
        if (pathError != null) {
            return new BashResult(-1, "", pathError, false, command);
        }

        String[] shell = OsDetector.getDefaultShell();
        String fullCommand = shell.length > 1 ? shell[1] + " " + command : command;

        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmdList = new ArrayList<>();
            for (String s : shell) {
                cmdList.add(s);
            }
            cmdList.add(command);
            pb.command(cmdList);
            pb.redirectErrorStream(false);

            Process process = pb.start();
            String processId = java.util.UUID.randomUUID().toString();
            runningProcesses.put(processId, process);

            boolean timedOut = false;
            try {
                timedOut = !process.waitFor(timeoutSec, TimeUnit.SECONDS);
                if (timedOut) {
                    process.destroyForcibly();
                    return new BashResult(-1, "", "Command timed out after " + timeoutSec + " seconds", true, command);
                }
            } finally {
                runningProcesses.remove(processId);
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            return new BashResult(process.exitValue(), stdout, stderr, false, command);

        } catch (IOException e) {
            return new BashResult(-1, "", "IO Error: " + e.getMessage(), false, command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BashResult(-1, "", "Interrupted: " + e.getMessage(), false, command);
        }
    }

    /**
     * Execute a command with arguments (safer, no shell parsing).
     * @param command Base command (e.g., "git", "ls")
     * @param args Command arguments
     * @return BashResult with exit code, stdout, stderr
     */
    public BashResult executeCommand(String command, List<String> args) {
        return executeCommand(command, args, timeoutSeconds);
    }

    /**
     * Execute a command with arguments and custom timeout.
     * @param command Base command (e.g., "git", "ls")
     * @param args Command arguments
     * @param timeoutSec Timeout in seconds
     * @return BashResult with exit code, stdout, stderr
     */
    public BashResult executeCommand(String command, List<String> args, int timeoutSec) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmdList = new ArrayList<>();
            cmdList.add(command);
            if (args != null) {
                cmdList.addAll(args);
            }
            pb.command(cmdList);
            pb.redirectErrorStream(false);

            Process process = pb.start();
            boolean timedOut = !process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (timedOut) {
                process.destroyForcibly();
                return new BashResult(-1, "", "Command timed out after " + timeoutSec + " seconds", true, command);
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            return new BashResult(process.exitValue(), stdout, stderr, false, command);

        } catch (IOException e) {
            return new BashResult(-1, "", "IO Error: " + e.getMessage(), false, command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BashResult(-1, "", "Interrupted: " + e.getMessage(), false, command);
        }
    }

    /**
     * MCP tool handler for execute_command.
     * This method is called via reflection by buildBuiltinTool.
     */
    public Mono<McpSchema.CallToolResult> handleToolCall(McpAsyncServerExchange exchange, Map<String, Object> arguments) {
        return handleExecuteCommand(exchange, arguments);
    }

    /**
     * Internal handler for execute_command.
     */
    private Mono<McpSchema.CallToolResult> handleExecuteCommand(McpAsyncServerExchange exchange, Map<String, Object> arguments) {
        String command = (String) arguments.get("command");
        @SuppressWarnings("unchecked")
        List<String> args = (List<String>) arguments.get("args");
        Integer timeout = arguments.get("timeout") != null ?
            Integer.valueOf(arguments.get("timeout").toString()) : null;

        BashResult result;
        if (args != null && !args.isEmpty()) {
            if (timeout != null) {
                result = executeCommand(command, args, timeout);
            } else {
                result = executeCommand(command, args);
            }
        } else {
            if (timeout != null) {
                result = executeCommand(command, timeout);
            } else {
                result = executeCommand(command);
            }
        }

        String output = result.getOutput();
        String status = result.isSuccess() ? "SUCCESS" : "FAILED";
        if (result.isTimedOut()) {
            status = "TIMEOUT";
        }

        String textOutput = String.format(
            "Exit Code: %d | Status: %s\n\n%s",
            result.getExitCode(), status, output
        );

        if (!result.isSuccess() && result.getStderr() != null && !result.getStderr().isEmpty()) {
            textOutput += "\nStderr:\n" + result.getStderr();
        }

        return Mono.just(McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(textOutput)))
            .build());
    }

    /**
     * Get the JSON schema for the execute_command tool.
     * This method is called via reflection by buildBuiltinTool.
     */
    public String getToolSchema() {
        return """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "The command to execute (e.g., 'ls -la', 'git status', 'dir')"
                },
                "args": {
                  "type": "array",
                  "items": {"type": "string"},
                  "description": "Optional command arguments for direct execution without shell parsing"
                },
                "timeout": {
                  "type": "integer",
                  "description": "Optional timeout in seconds (default: 30)"
                }
              },
              "required": ["command"]
            }
            """;
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        timeoutExecutor.shutdownNow();
        // Kill any remaining processes
        runningProcesses.values().forEach(p -> p.destroyForcibly());
        runningProcesses.clear();
    }
}
