#!/usr/bin/env python3
"""
MCP Streamable HTTP Transport Test

Tests FastMCP4J server using Streamable HTTP transport.

Server: SimpleStreamableServer (port 3002)
Transport: HTTP Streamable (bidirectional HTTP POST/SSE)

The streamable transport combines HTTP POST for sending messages
with Server-Sent Events for streaming responses.
"""
import asyncio
import sys
from typing import Optional

from mcp import ClientSession
from mcp.client.streamable_http import streamable_http_client


# Configuration
SERVER_URL: str = "http://127.0.0.1:3002/mcp"
MAX_RETRIES: int = 5
RETRY_DELAY: int = 2


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

    log_step("Testing 'echo' tool...")
    result = await session.call_tool("echo", {"message": "Hello CI/CD"})
    log_result("echo('Hello CI/CD')", result.content[0].text)

    return True


async def run_test() -> bool:
    """Run the Streamable HTTP transport test."""
    log_section("MCP Streamable HTTP Transport Test")
    print(f"Target: {SERVER_URL}")
    print(f"Max retries: {MAX_RETRIES}")

    for attempt in range(1, MAX_RETRIES + 1):
        try:
            log_step(f"Connection attempt {attempt}/{MAX_RETRIES}...")

            async with streamable_http_client(SERVER_URL) as (read_stream, write_stream, get_session_id):
                async with ClientSession(read_stream, write_stream) as session:
                    log_result("Connection established", f"Connected to {SERVER_URL}")

                    # Initialize session
                    await session.initialize()
                    print("  📋 Session initialized")

                    # Run tests
                    if await test_tools(session):
                        log_section("✅ ALL TESTS PASSED")
                        return True

        except Exception as e:
            print(f"\n  ❌ Attempt {attempt} failed: {e}")
            if attempt < MAX_RETRIES:
                print(f"  ⏳ Retrying in {RETRY_DELAY}s...")
                await asyncio.sleep(RETRY_DELAY)

    log_section("❌ TESTS FAILED")
    print("Max retries exceeded. See error above.")
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
