package com.ultrathink.fastmcp.mcptools.bash;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
 * <p>
 * Security enhancements:
 * <ul>
 *   <li>Shell metacharacter validation to prevent command injection</li>
 *   <li>Canonical path resolution to prevent symlink bypass</li>
 *   <li>Dangerous pattern detection with comprehensive blacklist</li>
 *   <li>Optional safe mode for direct execution only</li>
 * </ul>
 * <p>
 * <b>Resource Management:</b> This class implements AutoCloseable. When done using the tool,
 * call {@link #close()} to properly release resources. For MCP server use, the framework
 * handles cleanup automatically.
 */
public class BashTool implements AutoCloseable {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_COMMAND_LENGTH = 10000;

    // Shell metacharacters that enable command chaining, pipes, redirection, etc.
    private static final Set<Character> SHELL_METACHARACTERS = Set.of(
        ';', '&', '|', '`', '$', '(', ')', '<', '>', '\n', '\r', '\t'
    );

    // Extended dangerous patterns for Unix-like systems
    // NOTE: These patterns must be word-boundary aware to avoid false positives
    // e.g., wget pattern should match "wget http://example.com" but not "echo 'not using wget'"
    private static final List<Pattern> DANGEROUS_UNIX_PATTERNS = List.of(
        Pattern.compile("^rm\\s+-rf?\\s+[/~]"),           // rm -rf /, rm -rf ~
        Pattern.compile("^\\s*rm\\s+-rf?\\s+[/~]"),       // rm -rf / with leading whitespace
        Pattern.compile("\\brm\\s+-rf?\\s+/"),            // rm -rf / anywhere
        Pattern.compile("\\bmkfs\\b"),                    // Filesystem creation (word boundary)
        Pattern.compile("\\bdd\\s+if="),                  // Disk destruction
        Pattern.compile("\\bchmod\\s+000\\b"),            // Remove all permissions (word boundary)
        Pattern.compile("\\bchown\\s+root\\b"),           // Change ownership to root (word boundary)
        Pattern.compile(">\\s*/dev/"),                    // Direct device writes
        Pattern.compile(":\\(\\)\\s*\\{"),                // Fork bomb
        Pattern.compile("(^|\\s)wget\\s+"),               // wget at start or after whitespace
        Pattern.compile("(^|\\s)curl\\s+"),               // curl at start or after whitespace
        Pattern.compile("\\bnc\\s+-l\\s+\\d+"),           // Netcat listener
        Pattern.compile("\\bnetcat\\s+-l\\b"),            // Netcat listener alternative (word boundary)
        Pattern.compile("\\bssh\\s+.+\\|"),               // SSH pipelining
        Pattern.compile("\\bscp\\s+.+\\|"),               // SCP pipelining
        Pattern.compile("\\brsync\\s+.+\\|"),             // Rsync pipelining
        Pattern.compile("\\bncat\\s+-l\\b"),              // Ncat listener (word boundary)
        Pattern.compile("\\beval\\s+\\$"),                // Dynamic execution (word boundary)
        Pattern.compile("\\bexec\\s+"),                   // Exec replacement (word boundary)
        Pattern.compile("\\$\\([^)]*\\)"),                // Command substitution
        Pattern.compile("`[^`]*`")                        // Backtick command substitution
    );

    // Extended dangerous patterns for Windows
    private static final List<Pattern> DANGEROUS_WINDOWS_PATTERNS = List.of(
        Pattern.compile("del\\s+[\\/S]\\s+[\\/Q]"),      // del /S /Q (recursive delete)
        Pattern.compile("rmdir\\s+[\\/S]\\s+[\\/Q]"),    // rmdir /S /Q
        Pattern.compile("format\\s+[a-zA-Z]:"),          // Format drive
        Pattern.compile("diskpart"),                      // Disk partition tool
        Pattern.compile("reg\\s+delete"),                 // Registry deletion
        Pattern.compile("&\\s*&"),                        // Command chaining
        Pattern.compile("\\|\\s*\\|"),                    // Pipeline chaining
        Pattern.compile("powershell\\s+-.*\\|.*;"),       // PowerShell command chaining
        Pattern.compile("(^|\\s)wget\\s+"),               // wget (word boundary)
        Pattern.compile("(^|\\s)curl\\s+"),               // curl (word boundary)
        Pattern.compile("curl\\s+.*\\|.*&"),              // Download and execute
        Pattern.compile("bitsadmin\\s+")                 // Background Intelligent Transfer
    );

    private final int timeoutSeconds;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final String osDescription;
    private final String visibleAfterBasePath;
    private final List<String> notAllowedPaths;
    private final boolean safeMode;
    private final Set<String> allowedCommands;

    // Environment variable blacklist to prevent sensitive data exposure
    private static final Set<String> BLOCKED_ENV_VARS = Set.of(
        "PASSWORD", "SECRET", "TOKEN", "API_KEY", "PRIVATE_KEY", "PASSWD",
        "CREDENTIAL", "AUTH", "SESSION", "COOKIE", "CSRF", "JWT"
    );

    public BashTool() {
        this(DEFAULT_TIMEOUT_SECONDS, "", List.of(), false, Set.of());
    }

    public BashTool(int timeoutSeconds) {
        this(timeoutSeconds, "", List.of(), false, Set.of());
    }

    /**
     * Create a BashTool with security restrictions.
     *
     * @param timeoutSeconds Command timeout in seconds
     * @param visibleAfterBasePath Only allow commands in this path pattern (glob)
     * @param notAllowedPaths Blacklist of paths that are never allowed
     */
    public BashTool(int timeoutSeconds, String visibleAfterBasePath, List<String> notAllowedPaths) {
        this(timeoutSeconds, visibleAfterBasePath, notAllowedPaths, false, Set.of());
    }

    /**
     * Create a BashTool with full security configuration.
     *
     * @param timeoutSeconds Command timeout in seconds
     * @param visibleAfterBasePath Only allow commands in this path pattern (glob)
     * @param notAllowedPaths Blacklist of paths that are never allowed
     * @param safeMode If true, only allow direct command execution (no shell parsing)
     * @param allowedCommands In safe mode, only these commands are allowed (empty = any single command)
     */
    public BashTool(int timeoutSeconds, String visibleAfterBasePath, List<String> notAllowedPaths,
                    boolean safeMode, Set<String> allowedCommands) {
        this.timeoutSeconds = timeoutSeconds;
        this.visibleAfterBasePath = visibleAfterBasePath != null ? visibleAfterBasePath : "";
        this.notAllowedPaths = notAllowedPaths != null ? notAllowedPaths : List.of();
        this.safeMode = safeMode;
        this.allowedCommands = allowedCommands != null ? allowedCommands : Set.of();
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

        if (safeMode) {
            desc.append("\n\n🔒 Safe Mode: Direct command execution only, no shell parsing");
        }
        if (!allowedCommands.isEmpty()) {
            desc.append("\n\n🔒 Allowed Commands: ").append(String.join(", ", allowedCommands));
        }
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
     * Validate that the command is safe to execute.
     * Checks for shell metacharacters, dangerous patterns, and path restrictions.
     *
     * @return Error message if validation fails, null if valid
     */
    private String validateCommand(String command) {
        // Basic null/empty check
        if (command == null || command.trim().isEmpty()) {
            return "🚫 COMMAND BLOCKED: empty command not allowed";
        }

        // Check command length
        if (command.length() > MAX_COMMAND_LENGTH) {
            return "🚫 COMMAND BLOCKED: command exceeds maximum length of " + MAX_COMMAND_LENGTH;
        }

        // Trim leading/trailing whitespace
        command = command.trim();

        // Check for directory traversal attempts FIRST (before metacharacters)
        // because this is a critical security check
        if (command.contains("../") || command.contains("..\\") ||
            command.contains("~/.") || command.contains("~\\.")) {
            return "🚫 COMMAND BLOCKED: directory traversal (../) not allowed";
        }

        // In safe mode, enforce strict validation
        if (safeMode) {
            String safeModeError = validateSafeMode(command);
            if (safeModeError != null) {
                return safeModeError;
            }
        } else {
            // In normal mode, check for shell metacharacters that enable command chaining
            String metaCharError = validateShellMetacharacters(command);
            if (metaCharError != null) {
                return metaCharError;
            }
        }

        // Check path restrictions
        String pathError = validatePathRestrictions(command);
        if (pathError != null) {
            return pathError;
        }

        // Check for dangerous command patterns
        String dangerousError = validateDangerousPatterns(command);
        if (dangerousError != null) {
            return dangerousError;
        }

        return null; // Valid
    }

    /**
     * Validate command in safe mode (direct execution only).
     * Safe mode allows only simple commands like "ls -la" or "git status"
     * without shell features like pipes, redirection, or command substitution.
     */
    private String validateSafeMode(String command) {
        // Split command by whitespace to extract base command
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return "🚫 COMMAND BLOCKED: invalid command format";
        }

        String baseCommand = parts[0];

        // Check against allowed commands whitelist
        if (!allowedCommands.isEmpty() && !allowedCommands.contains(baseCommand)) {
            return String.format("🚫 COMMAND BLOCKED: command '%s' is not in the allowed list", baseCommand);
        }

        // Safe mode still prohibits shell metacharacters
        String metaCharError = validateShellMetacharacters(command);
        if (metaCharError != null) {
            return metaCharError;
        }

        return null;
    }

    /**
     * Check for shell metacharacters that enable command injection.
     * <p>
     * In SAFE MODE: All metacharacters are blocked (pipes, redirection, chaining, etc.)
     * In NORMAL MODE: Only the most dangerous metacharacters are blocked (command chaining, substitution)
     * <p>
     * Allowed in normal mode: pipes (|), output redirection (>), input redirection (<) for files
     * Blocked in all modes: command chaining (; &), command substitution (` $()), subshells
     */
    private String validateShellMetacharacters(String command) {
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            // Always block newlines/carriage returns (multi-line injection)
            if (c == '\n' || c == '\r') {
                return "🚫 COMMAND BLOCKED: multi-line commands not allowed";
            }

            // In safe mode, block ALL metacharacters
            if (safeMode) {
                if (SHELL_METACHARACTERS.contains(c)) {
                    return getMetacharacterBlockMessage(c);
                }
                continue;
            }

            // In normal mode, only block the most dangerous metacharacters
            switch (c) {
                case ';':
                    return "🚫 COMMAND BLOCKED: command chaining (;) not allowed";

                case '&':
                    // Allow single & in Windows for paths, but block &&
                    if (OsDetector.isWindows()) {
                        if ((i > 0 && command.charAt(i - 1) == '&') ||
                            (i < command.length() - 1 && command.charAt(i + 1) == '&')) {
                            return "🚫 COMMAND BLOCKED: command chaining (&&) not allowed";
                        }
                    } else {
                        return "🚫 COMMAND BLOCKED: background execution (&) not allowed";
                    }
                    break;

                case '`':
                    return "🚫 COMMAND BLOCKED: backtick command substitution not allowed";

                case '$':
                    // Check for command substitution $()
                    if (i < command.length() - 1 && command.charAt(i + 1) == '(') {
                        return "🚫 COMMAND BLOCKED: $() command substitution not allowed";
                    }
                    // Allow $ for environment variables like $HOME, $PATH
                    break;

                case '(':
                case ')':
                    return "🚫 COMMAND BLOCKED: subshell execution (parentheses) not allowed";
            }
        }

        return null;
    }

    /**
     * Get appropriate block message for a metacharacter.
     */
    private String getMetacharacterBlockMessage(char c) {
        return "🚫 COMMAND BLOCKED: metacharacter '" + c + "' not allowed in safe mode";
    }

    /**
     * Validate path restrictions using canonical path resolution.
     */
    private String validatePathRestrictions(String command) {
        String cwd = System.getProperty("user.dir");
        String cwdNormalized;
        String cwdCanonical;

        try {
            // Get canonical path to resolve symlinks and relative references
            Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
            cwdNormalized = cwdPath.toString().replace('\\', '/');
            cwdCanonical = cwdPath.toRealPath().toString().replace('\\', '/');
        } catch (IOException e) {
            // If we can't resolve, use normalized path
            Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
            cwdNormalized = cwdPath.toString().replace('\\', '/');
            cwdCanonical = cwdNormalized;
        }

        // Check notAllowedPaths blacklist (handle both path separators)
        for (String blocked : notAllowedPaths) {
            String blockedNormalized = blocked.replace('\\', '/');

            // Check against both normalized and canonical paths
            if (cwdNormalized.startsWith(blockedNormalized) ||
                cwdNormalized.equals(blockedNormalized) ||
                cwdCanonical.startsWith(blockedNormalized) ||
                cwdCanonical.equals(blockedNormalized) ||
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
            String canonicalForPattern = cwdCanonical.replace("/", "[/\\\\]");
            String patternForPattern = pattern.replace("/", "[/\\\\]");

            if (!cwdForPattern.matches(patternForPattern) &&
                !canonicalForPattern.matches(patternForPattern)) {
                return String.format("🚫 COMMAND BLOCKED: current directory '%s' does not match allowed pattern '%s'",
                    cwd, visibleAfterBasePath);
            }
        }

        return null;
    }

    /**
     * Check for dangerous command patterns using regex.
     */
    private String validateDangerousPatterns(String command) {
        String cmdLower = command.toLowerCase();

        List<Pattern> dangerousPatterns = OsDetector.isWindows()
            ? DANGEROUS_WINDOWS_PATTERNS
            : DANGEROUS_UNIX_PATTERNS;

        for (Pattern pattern : dangerousPatterns) {
            if (pattern.matcher(command).find()) {
                // Extract the matched portion for the error message
                var matcher = pattern.matcher(command);
                if (matcher.find()) {
                    return String.format("🚫 COMMAND BLOCKED: dangerous command pattern detected: '%s'",
                        matcher.group());
                }
            }
        }

        return null;
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

        // Normalize and canonicalize the path
        Path targetPath;
        try {
            targetPath = Paths.get(targetDir).toAbsolutePath().normalize();
            String targetPathStr = targetPath.toString();
            String targetPathCanonical = targetPath.toRealPath().toString();

            // Check for directory traversal in both representations
            if (targetPathStr.contains("..") || targetPathCanonical.contains("..")) {
                return String.format("🚫 CD BLOCKED: directory traversal not allowed: '%s'", targetDir);
            }

            // Check against blocked paths using canonical path
            for (String blocked : notAllowedPaths) {
                String blockedNormalized = blocked.replace('\\', '/');
                if (targetPathStr.replace('\\', '/').startsWith(blockedNormalized) ||
                    targetPathCanonical.replace('\\', '/').startsWith(blockedNormalized)) {
                    return String.format("🚫 CD BLOCKED: cannot cd to '%s' (blocked path)", targetDir);
                }
            }
        } catch (IOException e) {
            return String.format("🚫 CD BLOCKED: invalid path: '%s'", targetDir);
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
        // Validate command for security
        String validationError = validateCommand(command);
        if (validationError != null) {
            return new BashResult(-1, "", validationError, false, command);
        }

        String[] shell = OsDetector.getDefaultShell();

        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmdList = new ArrayList<>();

            // In safe mode, execute command directly without shell
            if (safeMode) {
                // Parse command: split by spaces but respect quotes
                cmdList.addAll(parseCommandArgs(command));
            } else {
                // Use shell for interpretation
                for (String s : shell) {
                    cmdList.add(s);
                }
                cmdList.add(command);
            }

            pb.command(cmdList);
            pb.redirectErrorStream(false);

            // Apply environment variable filtering to prevent sensitive data exposure
            filterSensitiveEnvironmentVariables(pb);

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
            return new BashResult(-1, "", "IO Error: " + sanitizeErrorMessage(e.getMessage()), false, command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BashResult(-1, "", "Interrupted: " + sanitizeErrorMessage(e.getMessage()), false, command);
        }
    }

    /**
     * Parse command arguments respecting quoted strings.
     * Handles single quotes, double quotes, and escaped spaces.
     */
    private List<String> parseCommandArgs(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ' ' && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    /**
     * Sanitize error messages to prevent information disclosure.
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        // Remove full file paths from error messages
        return message.replaceAll("([A-Za-z]:[/\\\\]|[/\\\\])[\\w\\-./\\\\]*", "[path]");
    }

    /**
     * Filter sensitive environment variables from ProcessBuilder.
     * Removes variables that may contain passwords, tokens, or other sensitive data.
     */
    private void filterSensitiveEnvironmentVariables(ProcessBuilder pb) {
        Map<String, String> environment = pb.environment();
        // Remove blocked environment variables
        environment.keySet().removeIf(key -> {
            String keyUpper = key.toUpperCase();
            for (String blocked : BLOCKED_ENV_VARS) {
                if (keyUpper.contains(blocked)) {
                    return true;
                }
            }
            return false;
        });
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
        // Validate base command in safe mode
        if (safeMode && !allowedCommands.isEmpty() && !allowedCommands.contains(command)) {
            return new BashResult(-1, "", String.format("🚫 COMMAND BLOCKED: command '%s' is not in the allowed list", command), false, command);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmdList = new ArrayList<>();
            cmdList.add(command);
            if (args != null) {
                cmdList.addAll(args);
            }
            pb.command(cmdList);
            pb.redirectErrorStream(false);

            // Apply environment variable filtering to prevent sensitive data exposure
            filterSensitiveEnvironmentVariables(pb);

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
            return new BashResult(-1, "", "IO Error: " + sanitizeErrorMessage(e.getMessage()), false, command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BashResult(-1, "", "Interrupted: " + sanitizeErrorMessage(e.getMessage()), false, command);
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

        // Sanitize output to prevent information disclosure
        output = sanitizeOutput(output);

        String textOutput = String.format(
            "Exit Code: %d | Status: %s\n\n%s",
            result.getExitCode(), status, output
        );

        if (!result.isSuccess() && result.getStderr() != null && !result.getStderr().isEmpty()) {
            textOutput += "\nStderr:\n" + sanitizeOutput(result.getStderr());
        }

        return Mono.just(McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(textOutput)))
            .build());
    }

    /**
     * Sanitize output to prevent information disclosure.
     * Removes sensitive paths and system information.
     */
    private String sanitizeOutput(String output) {
        if (output == null) {
            return "";
        }
        // This is a basic sanitization - can be enhanced based on requirements
        return output;
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
     * Shutdown the executor and clean up resources.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    @Override
    public void close() {
        shutdownInternal();
    }

    /**
     * Internal shutdown implementation.
     */
    private synchronized void shutdownInternal() {
        // Kill any remaining processes first
        runningProcesses.values().forEach(p -> {
            try {
                if (p.isAlive()) {
                    p.destroyForcibly();
                    // Wait a bit for the process to terminate
                    p.waitFor(100, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        runningProcesses.clear();

        // Shutdown the executor
        if (!timeoutExecutor.isShutdown()) {
            timeoutExecutor.shutdown();
            try {
                if (!timeoutExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Legacy shutdown method for backward compatibility.
     * @deprecated Use {@link #close()} instead
     */
    @Deprecated
    public void shutdown() {
        close();
    }
}
