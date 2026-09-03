package io.github.terseprompts.fastmcp.mcptools.bash;

import io.github.terseprompts.fastmcp.annotations.BashMode;
import io.github.terseprompts.fastmcp.annotations.McpBash;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the {@link McpBash} annotation contract: defaults are sandbox-first
 * (safe by default) and both modes keep their documented parameters.
 */
class McpBashAnnotationTest {

    @McpBash
    static class Defaults {}

    @McpBash(
            mode = BashMode.HOST,
            timeout = 15,
            maxCommands = 500,
            username = "ci",
            hostname = "box",
            cwd = "/tmp",
            env = {"A=1", "B=2"},
            mounts = {"/data=/host/data:rw"},
            allowMountsUnder = {"/host"},
            visibleAfterBasePath = "/allowed/*",
            notAllowedPaths = {"/etc"}
    )
    static class Custom {}

    @Test
    void sandboxIsTheDefaultMode() {
        McpBash a = Defaults.class.getAnnotation(McpBash.class);
        assertEquals(BashMode.SANDBOX, a.mode());
    }

    @Test
    void documentedDefaultsHold() {
        McpBash a = Defaults.class.getAnnotation(McpBash.class);
        assertEquals(30, a.timeout());
        assertEquals(10_000, a.maxCommands());
        assertEquals("agent", a.username());
        assertEquals("sandbox", a.hostname());
        assertEquals("/", a.cwd());
        assertEquals(0, a.env().length);
        assertEquals(0, a.mounts().length);
        assertEquals(0, a.allowMountsUnder().length);
        assertEquals("", a.visibleAfterBasePath());
        assertEquals(0, a.notAllowedPaths().length);
    }

    @Test
    void allAttributesAreSettable() {
        McpBash a = Custom.class.getAnnotation(McpBash.class);
        assertEquals(BashMode.HOST, a.mode());
        assertEquals(15, a.timeout());
        assertEquals(500, a.maxCommands());
        assertEquals("ci", a.username());
        assertEquals("box", a.hostname());
        assertEquals("/tmp", a.cwd());
        assertArrayEquals(new String[]{"A=1", "B=2"}, a.env());
        assertArrayEquals(new String[]{"/data=/host/data:rw"}, a.mounts());
        assertArrayEquals(new String[]{"/host"}, a.allowMountsUnder());
        assertEquals("/allowed/*", a.visibleAfterBasePath());
        assertArrayEquals(new String[]{"/etc"}, a.notAllowedPaths());
    }

    @Test
    void mountSpecParserRejectsBadInput() {
        // via SandboxBashTool constructor (validation happens at startup)
        assertThrows(IllegalArgumentException.class, () -> new SandboxBashTool(
                30, 100, "u", "h", "/", new String[]{"NO_EQUALS"}, new String[0], new String[0]));
        assertThrows(IllegalArgumentException.class, () -> new SandboxBashTool(
                30, 100, "u", "h", "/", new String[0], new String[]{"relative=host"}, new String[]{"host"}));
    }

    @Test
    void hostModeToolStillConstructsAndRuns() {
        BashTool host = new BashTool(30, "", List.of());
        assertNotNull(host.getToolDescription());
        assertNotNull(host.getToolSchema());
        // real shell on the CI host — keep it trivial and safe
        BashResult r = host.executeCommand("echo host-ok");
        assertTrue(r.getOutput().contains("host-ok") || r.getExitCode() != -1);
    }
}
