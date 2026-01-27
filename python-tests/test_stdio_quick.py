#!/usr/bin/env python3
"""
MCP STDIO Transport Test

Tests FastMCP4J server using STDIO (standard input/output) transport.

Server: StdioFullServer (spawned via mvn exec:java)
Transport: STDIO
"""
import asyncio
import sys

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


# Configuration
SERVER_CLASS: str = "com.ultrathink.fastmcp.example.StdioFullServer"


def log_section(title: str) -> None:
    """Print a formatted section header."""
    print(f"\n{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")


def log_step(step: str) -> None:
    """Print a step indicator."""
    print(f"\n>>> {step}")


def log_result(test: str, result: str, success: bool = True) -> None:
    """Print a test result."""
    status = "✅" if success else "❌"
    print(f"  {status} {test}: {result}")


async def test_tools(session: ClientSession) -> bool:
    """Test tool listing and invocation."""
    log_step("Listing available tools...")
    tools = await session.list_tools()
    print(f"  Found {len(tools.tools)} tools:")
    for t in tools.tools:
        print(f"    - {t.name}")

    log_step("Testing 'add' tool...")
    result = await session.call_tool("add", {"a": 10, "b": 5})
    log_result("add(10, 5)", result.content[0].text)

    log_step("Testing 'asyncTask' tool...")
    result = await session.call_tool("asyncTask", {"taskName": "test"})
    log_result("asyncTask('test')", result.content[0].text)

    log_step("Testing 'memory' tool...")
    await session.call_tool("memory", {"mode": "write", "key": "test", "value": "Hello"})
    result = await session.call_tool("memory", {"mode": "read", "key": "test"})
    log_result("memory read", result.content[0].text)

    return True


async def run_test() -> bool:
    """Run the STDIO transport test."""
    log_section("MCP STDIO Transport Test")
    print(f"Server class: {SERVER_CLASS}")

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

                # Initialize session
                await session.initialize()
                print("  📋 Session initialized")

                # Run tests
                if await test_tools(session):
                    log_section("✅ ALL TESTS PASSED")
                    return True

    except Exception as e:
        log_section("❌ TESTS FAILED")
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False


def main() -> int:
    """Entry point."""
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
