#!/usr/bin/env python3
"""
MCP STDIO Transport Test — Sandboxed Bash

Tests FastMCP4J sandboxed bash tool (bashkit4j) using STDIO transport.

Server: BashDemoServer (spawned via mvn exec:java)
Transport: STDIO

Verifies:
  - tools/list exposes the "bash" tool
  - echo round-trip inside the sandbox
  - state persists across calls (cwd, files)
  - the sandbox filesystem is NOT the host filesystem
"""
import asyncio
import sys

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


SERVER_CLASS: str = "io.github.terseprompts.fastmcp.example.BashDemoServer"


def log_section(title: str) -> None:
    print(f"\n{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")


def log_step(step: str) -> None:
    print(f"\n>>> {step}")


def log_result(test: str, result: str, success: bool = True) -> None:
    status = "✅" if success else "❌"
    print(f"  {status} {test}: {result}")


async def bash(session: ClientSession, script: str, **kwargs) -> str:
    """Call the sandboxed bash tool and return its text output."""
    result = await session.call_tool("bash", {"command": script, **kwargs})
    return result.content[0].text


async def test_bash(session: ClientSession) -> bool:
    """Exercise the sandboxed bash tool."""
    ok = True

    log_step("Listing tools...")
    tools = await session.list_tools()
    names = [t.name for t in tools.tools]
    print(f"  Found tools: {names}")
    if "bash" not in names:
        log_result("tools/list", "'bash' tool missing", success=False)
        return False
    log_result("tools/list", "'bash' tool exposed")

    log_step("Sandbox echo round-trip...")
    out = await bash(session, "echo bashkit-$(echo roundtrip)")
    if "bashkit-roundtrip" not in out or "SUCCESS" not in out:
        log_result("echo", out, success=False)
        ok = False
    else:
        log_result("echo", "pipes + substitution work in sandbox")

    log_step("State persistence across calls (cwd + file)...")
    await bash(session, "mkdir -p /work && cd /work && echo keepme > note.txt")
    out = await bash(session, "pwd && cat /work/note.txt")
    if "/work" in out and "keepme" in out:
        log_result("persistence", "cwd and files persist across calls")
    else:
        log_result("persistence", out, success=False)
        ok = False

    log_step("Host isolation (sandbox fs is not the host fs)...")
    out = await bash(session, "ls /")
    # 'python-tests' exists in the host repo checkout the server runs from;
    # it must never appear in the sandbox root listing.
    if "python-tests" in out or "pom.xml" in out:
        log_result("isolation", f"host files leaked into sandbox:\n{out}", success=False)
        ok = False
    else:
        log_result("isolation", "sandbox root shows no host files")

    log_step("Exit codes propagate...")
    out = await bash(session, "exit 7")
    if "Exit Code: 7" in out:
        log_result("exit codes", "exit 7 → Exit Code: 7")
    else:
        log_result("exit codes", out, success=False)
        ok = False

    log_step("Timeout enforcement...")
    out = await bash(session, "sleep 10", timeout=1)
    if "TIMEOUT" in out:
        log_result("timeout", "sleep 10 with timeout=1 → TIMEOUT")
    else:
        log_result("timeout", out, success=False)
        ok = False

    return ok


async def run_test() -> bool:
    """Connect to the sandboxed bash demo server over STDIO and run tests."""
    log_section("FastMCP4J Sandboxed Bash — STDIO Transport Test")

    server_params = StdioServerParameters(
        command="mvn",
        args=["-q", "exec:java",
              f"-Dexec.mainClass={SERVER_CLASS}",
              "-Dexec.classpathScope=test"],
    )

    try:
        log_step("Starting server via Maven...")
        async with stdio_client(server_params) as (read, write):
            async with ClientSession(read, write) as session:
                log_result("Connection established", "STDIO client connected")
                await session.initialize()
                print("  📋 Session initialized")

                if await test_bash(session):
                    log_section("✅ ALL TESTS PASSED")
                    return True

    except Exception as e:
        log_section("❌ TESTS FAILED")
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False

    log_section("❌ TESTS FAILED")
    return False


def main() -> int:
    try:
        result = asyncio.run(run_test())
        return 0 if result else 1
    except KeyboardInterrupt:
        print("\n\n⚠️  Test interrupted by user")
        return 130
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
