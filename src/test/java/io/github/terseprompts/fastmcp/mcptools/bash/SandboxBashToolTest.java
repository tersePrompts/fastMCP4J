package io.github.terseprompts.fastmcp.mcptools.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SandboxBashTool} — the bashkit4j-backed sandbox mode.
 */
class SandboxBashToolTest {

    private SandboxBashTool tool() {
        return new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                new String[0], new String[0], new String[0]);
    }

    // ------------------------------------------------------------ basics

    @Test
    void echoRoundTrip() {
        BashResult r = tool().executeCommand("echo sandbox-ok");
        assertEquals(0, r.getExitCode());
        assertTrue(r.getOutput().contains("sandbox-ok"));
    }

    @Test
    void pipesAndSubstitutionWork() {
        BashResult r = tool().executeCommand("printf 'b\\na\\n' | sort | tr a-z A-Z");
        assertEquals(0, r.getExitCode());
        assertTrue(r.getOutput().contains("A") && r.getOutput().contains("B"));

        BashResult r2 = tool().executeCommand("echo sub-$(echo stitution)");
        assertEquals(0, r2.getExitCode());
        assertTrue(r2.getOutput().contains("sub-stitution"));
    }

    @Test
    void exitCodesPropagate() {
        assertEquals(3, tool().executeCommand("exit 3").getExitCode());
        assertEquals(1, tool().executeCommand("false").getExitCode());
        assertEquals(0, tool().executeCommand("true").getExitCode());
    }

    @Test
    void emptyScriptRejected() {
        BashResult r = tool().executeCommand("   ");
        assertEquals(-1, r.getExitCode());
        assertTrue(r.getStderr().contains("empty"));
    }

    // ------------------------------------------------------- sandbox state

    @Test
    void statePersistsAcrossCalls() {
        SandboxBashTool t = tool();
        assertEquals(0, t.executeCommand("mkdir -p /work && cd /work && touch marker").getExitCode());
        BashResult pwd = t.executeCommand("pwd");
        assertTrue(pwd.getOutput().contains("/work"));
        BashResult ls = t.executeCommand("ls /work");
        assertTrue(ls.getOutput().contains("marker"));
    }

    @Test
    void envSeedingWorks() {
        SandboxBashTool t = new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                new String[]{"AGENT_NAME=jules"}, new String[0], new String[0]);
        assertTrue(t.executeCommand("echo $AGENT_NAME").getOutput().contains("jules"));
    }

    @Test
    void javaSideSandboxFileRoundTrip() {
        SandboxBashTool t = tool();
        assertEquals(0, t.executeCommand("echo virtual > /note.txt").getExitCode());
        assertTrue(t.readSandboxFile("/note.txt").contains("virtual"));
    }

    // ------------------------------------------------------------ isolation

    @Test
    void sandboxIdentityIsVirtual() {
        BashResult who = tool().executeCommand("whoami");
        assertTrue(who.getOutput().contains("agent"));
        BashResult host = tool().executeCommand("hostname");
        assertTrue(host.getOutput().contains("sandbox"));
    }

    @Test
    void virtualRootIsNotTheHostRoot() {
        // The host working directory (a Maven project) contains pom.xml and src/.
        // The virtual root must not show them.
        BashResult ls = tool().executeCommand("ls /");
        assertEquals(0, ls.getExitCode());
        assertFalse(ls.getOutput().contains("pom.xml"));
        assertFalse(ls.getOutput().contains("target"));

        BashResult pwd = tool().executeCommand("pwd");
        assertTrue(pwd.getOutput().contains("/"));

        // The root is a private, writable virtual filesystem — a marker file
        // created here lives only inside the sandbox.
        BashResult marker = tool().executeCommand("touch /probe-marker && ls /");
        assertTrue(marker.getOutput().contains("probe-marker"));
    }

    // --------------------------------------------------------------- mounts

    @Test
    void readOnlyMountCanReadButNotWrite(@TempDir Path hostDir) throws Exception {
        Files.writeString(hostDir.resolve("data.txt"), "mounted-content");
        SandboxBashTool t = new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                new String[0],
                new String[]{"/data=" + hostDir.toString()},
                new String[]{hostDir.toString()});

        assertTrue(t.executeCommand("cat /data/data.txt").getOutput().contains("mounted-content"));

        BashResult write = t.executeCommand("echo nope > /data/blocked.txt 2>/dev/null; exit $?");
        assertNotEquals(0, write.getExitCode());
        assertFalse(Files.exists(hostDir.resolve("blocked.txt")));
    }

    @Test
    void readWriteMountRoundTripsToHost(@TempDir Path hostDir) {
        SandboxBashTool t = new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                new String[0],
                new String[]{"/data=" + hostDir.toString() + ":rw"},
                new String[]{hostDir.toString()});

        assertEquals(0, t.executeCommand("echo from-sandbox > /data/out.txt").getExitCode());
        try {
            assertTrue(Files.readString(hostDir.resolve("out.txt")).contains("from-sandbox"));
        } catch (Exception e) {
            fail("host file not written by sandbox: " + e.getMessage());
        }
    }

    @Test
    void mountsRequireAllowlistPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                        new String[0], new String[]{"/x=C:/somewhere"}, new String[0]));
    }

    @Test
    void malformedMountSpecRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                        new String[0], new String[]{"/no-equals-sign"}, new String[]{"/tmp"}));
    }

    @Test
    void mountedFileReadableFromJava(@TempDir Path hostDir) throws Exception {
        Files.writeString(hostDir.resolve("j2s.txt"), "host-to-sandbox");
        SandboxBashTool t = new SandboxBashTool(30, 10_000, "agent", "sandbox", "/",
                new String[0],
                new String[]{"/data=" + hostDir.toString()},
                new String[]{hostDir.toString()});
        assertTrue(t.executeCommand("cat /data/j2s.txt").getOutput().contains("host-to-sandbox"));
        assertTrue(new String(Files.readAllBytes(hostDir.resolve("j2s.txt")),
                StandardCharsets.UTF_8).contains("host-to-sandbox"));
    }

    // -------------------------------------------------------------- timeout

    @Test
    void timeoutReturnsTimedOutResult() {
        long start = System.currentTimeMillis();
        BashResult r = tool().executeCommand("sleep 5", 1);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(r.isTimedOut());
        assertEquals(-1, r.getExitCode());
        assertTrue(elapsed < 4000, "should return near the 1s timeout, took " + elapsed + "ms");
    }

    @Test
    void sandboxIsReplacedAfterTimeout() {
        SandboxBashTool t = tool();
        BashResult timedOut = t.executeCommand("sleep 4", 1);
        assertTrue(timedOut.isTimedOut());

        // the retired sandbox must not poison the next call: a fresh sandbox
        // serves it, with no leftover state from the timed-out one
        BashResult fresh = t.executeCommand("echo fresh-sandbox");
        assertEquals(0, fresh.getExitCode());
        assertTrue(fresh.getOutput().contains("fresh-sandbox"));
    }

    @Test
    void concurrentCallsAllComplete() throws Exception {
        SandboxBashTool t = tool();
        java.util.List<java.util.concurrent.Callable<Boolean>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int n = i;
            tasks.add(() -> {
                for (int k = 0; k < 3; k++) {
                    BashResult r = t.executeCommand("echo thread-" + n);
                    if (r.getExitCode() != 0 || !r.getOutput().contains("thread-" + n)) {
                        return false;
                    }
                }
                return true;
            });
        }
        var futures = java.util.concurrent.Executors.newFixedThreadPool(4)
                .invokeAll(tasks);
        for (var f : futures) {
            assertTrue(f.get(), "concurrent sandbox call failed");
        }
    }

    @Test
    void closedToolRejectsCalls() {
        SandboxBashTool t = tool();
        t.close();
        assertThrows(IllegalStateException.class, () -> t.executeCommand("echo after-close"));
    }

    // --------------------------------------------------------- description/schema

    @Test
    void schemaAndDescriptionAreSandboxThemed() {
        SandboxBashTool t = tool();
        String schema = t.getToolSchema();
        assertTrue(schema.contains("\"command\""));
        assertTrue(schema.contains("draft/2020-12"));

        String desc = t.getToolDescription();
        assertTrue(desc.toLowerCase().contains("sandbox"));
        assertTrue(desc.contains("agent@sandbox"));
    }
}
