package com.ultrathink.fastmcp.mcptools.bash;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BashTool.
 */
class BashToolTest {

    @Test
    void testOsDetection() {
        OsType osType = OsDetector.getOsType();
        assertNotNull(osType);
        assertNotEquals(OsType.UNKNOWN, osType);

        // Check consistency
        assertEquals(OsDetector.isWindows(), osType == OsType.WINDOWS);
        assertEquals(OsDetector.isUnix(), osType == OsType.LINUX || osType == OsType.MACOS);
    }

    @Test
    void testGetDefaultShell() {
        String[] shell = OsDetector.getDefaultShell();
        assertNotNull(shell);
        assertTrue(shell.length >= 2);

        if (OsDetector.isWindows()) {
            assertTrue(shell[0].contains("cmd") || shell[0].contains("powershell"));
        } else {
            assertTrue(shell[0].contains("/bin/"));
        }
    }

    @Test
    void testGetShellName() {
        String shellName = OsDetector.getShellName();
        assertNotNull(shellName);
        assertFalse(shellName.isEmpty());
    }

    @Test
    void testExecuteEchoCommand() {
        BashTool bashTool = new BashTool();
        BashResult result = bashTool.executeCommand(OsDetector.isWindows() ? "echo hello" : "echo 'hello'");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Command should succeed: " + result);
        assertTrue(result.getStdout().contains("hello"), "Output should contain 'hello': " + result.getStdout());
        assertEquals(0, result.getExitCode());
        assertFalse(result.isTimedOut());
    }

    @Test
    void testExecuteCommandWithArgs() {
        BashTool bashTool = new BashTool();

        // Use echo as a safe command that accepts args
        String cmd = OsDetector.isWindows() ? "cmd.exe" : "echo";
        List<String> args = OsDetector.isWindows() ? List.of("/c", "echo", "test") : List.of("test");

        BashResult result = bashTool.executeCommand(cmd, args);

        assertNotNull(result);
        assertTrue(result.isSuccess() || result.getExitCode() == 0, "Command should succeed");
    }

    @Test
    void testGetPlatformInfo() {
        BashTool bashTool = new BashTool();
        Map<String, String> info = bashTool.getPlatformInfo();

        assertNotNull(info);
        assertFalse(info.isEmpty());
        assertTrue(info.containsKey("os"));
        assertTrue(info.containsKey("osType"));
        assertTrue(info.containsKey("defaultShell"));
        assertTrue(info.containsKey("javaVersion"));

        // Verify OS type is valid
        String osType = info.get("osType");
        assertTrue(osType.equals("WINDOWS") || osType.equals("LINUX") ||
                    osType.equals("MACOS") || osType.equals("UNKNOWN"));
    }

    @Test
    void testGetToolDescription() {
        BashTool bashTool = new BashTool();
        String description = bashTool.getToolDescription();

        assertNotNull(description);
        assertFalse(description.isEmpty());

        // Verify description contains OS info
        assertTrue(description.contains("Platform:"));
        assertTrue(description.contains("Shell:"));
        assertTrue(description.contains("OS Type:"));

        // Verify OS type is mentioned
        OsType osType = OsDetector.getOsType();
        assertTrue(description.contains(osType.name()));
    }

    @Test
    void testBashResultToString() {
        BashResult result = new BashResult(0, "stdout content", "stderr content", false, "test command");
        String str = result.toString();

        assertNotNull(str);
        assertTrue(str.contains("test command"));
        assertTrue(str.contains("Exit Code: 0"));
        assertTrue(str.contains("stdout content"));
        assertTrue(str.contains("stderr content"));
    }

    @Test
    void testBashResultIsSuccess() {
        BashResult success = new BashResult(0, "output", "", false, "cmd");
        assertTrue(success.isSuccess());

        BashResult failure = new BashResult(1, "", "error", false, "cmd");
        assertFalse(failure.isSuccess());

        BashResult timeout = new BashResult(-1, "", "", true, "cmd");
        assertFalse(timeout.isSuccess());
    }

    @Test
    void testBashResultGetOutput() {
        BashResult onlyStdout = new BashResult(0, "stdout", "", false, "cmd");
        assertEquals("stdout", onlyStdout.getOutput());

        BashResult onlyStderr = new BashResult(0, "", "stderr", false, "cmd");
        assertEquals("stderr", onlyStderr.getOutput());

        BashResult both = new BashResult(0, "stdout", "stderr", false, "cmd");
        String output = both.getOutput();
        assertTrue(output.contains("stdout"));
        assertTrue(output.contains("stderr"));
    }

    @Test
    void testTimeout() {
        BashTool bashTool = new BashTool(2); // 2 second timeout

        // Create a long-running command
        String longCommand = OsDetector.isWindows()
            ? "timeout 5"
            : "sleep 5";

        BashResult result = bashTool.executeCommand(longCommand);

        assertNotNull(result);
        assertTrue(result.isTimedOut(), "Command should time out");
        assertEquals(-1, result.getExitCode());
        assertTrue(result.getStderr().contains("timed out"));
    }

    @Test
    void testInvalidCommand() {
        BashTool bashTool = new BashTool();
        BashResult result = bashTool.executeCommand("nonexistentcommand12345");

        assertNotNull(result);
        // Exit code should be non-zero (or -1 on Windows if command not found)
        assertTrue(result.getExitCode() != 0 || result.getStderr().length() > 0);
        assertFalse(result.isSuccess());
    }

    @Test
    void testExecuteCommandSchema() {
        String executeSchema = BashTool.getExecuteCommandSchema();
        assertNotNull(executeSchema);
        assertTrue(executeSchema.contains("command"));
        assertTrue(executeSchema.contains("type"));
        assertTrue(executeSchema.contains("required"));
        assertTrue(executeSchema.contains("args"));
        assertTrue(executeSchema.contains("timeout"));
    }

    @Test
    void testPwdCommand() {
        if (!OsDetector.isWindows()) {
            BashTool bashTool = new BashTool();
            BashResult result = bashTool.executeCommand("pwd");

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNotNull(result.getStdout());
            assertFalse(result.getStdout().trim().isEmpty());
        }
    }

    @Test
    void testListDirectory() {
        BashTool bashTool = new BashTool();
        String command = OsDetector.isWindows() ? "dir" : "ls";

        BashResult result = bashTool.executeCommand(command);

        assertNotNull(result);
        assertTrue(result.isSuccess() || result.getExitCode() == 0);
        assertNotNull(result.getStdout());
    }
}
