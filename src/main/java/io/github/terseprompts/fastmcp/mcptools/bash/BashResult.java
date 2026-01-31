package io.github.terseprompts.fastmcp.mcptools.bash;

/**
 * Result of a bash/shell command execution.
 */
public class BashResult {

    /** Exit code from the command (0 = success) */
    private final int exitCode;

    /** Standard output from the command */
    private final String stdout;

    /** Standard error output from the command */
    private final String stderr;

    /** Whether the command timed out */
    private final boolean timedOut;

    /** Command that was executed */
    private final String command;

    public BashResult(int exitCode, String stdout, String stderr, boolean timedOut, String command) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.timedOut = timedOut;
        this.command = command;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public String getCommand() {
        return command;
    }

    /**
     * Check if the command executed successfully (exit code 0).
     */
    public boolean isSuccess() {
        return exitCode == 0 && !timedOut;
    }

    /**
     * Get a combined output (stdout + stderr if non-empty).
     */
    public String getOutput() {
        if (stderr == null || stderr.isEmpty()) {
            return stdout;
        }
        if (stdout.isEmpty()) {
            return stderr;
        }
        return stdout + "\n" + stderr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Command: ").append(command).append("\n");
        sb.append("Exit Code: ").append(exitCode);
        if (timedOut) {
            sb.append(" (TIMED OUT)");
        }
        sb.append("\n");
        if (!stdout.isEmpty()) {
            sb.append("STDOUT:\n").append(stdout).append("\n");
        }
        if (!stderr.isEmpty()) {
            sb.append("STDERR:\n").append(stderr).append("\n");
        }
        return sb.toString();
    }
}
