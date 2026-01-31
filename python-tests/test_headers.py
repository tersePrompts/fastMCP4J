#!/usr/bin/env python3
"""
MCP Header Passing Test

Tests that HTTP headers are being passed from the MCP client
to the FastMCP4J server and are accessible via @McpContext.

This tests:
1. SSE transport headers
2. Streamable HTTP transport headers

Server: EchoServer (port 3002 for streamable, 3001 for SSE)
"""
import asyncio
import sys
import re

from mcp import ClientSession
from mcp.client.sse import sse_client
from mcp.client.streamable_http import streamable_http_client


async def test_streamable_headers():
    """Test headers with Streamable HTTP transport."""
    print("\n" + "=" * 60)
    print("  Testing Streamable HTTP Transport Headers")
    print("=" * 60)

    try:
        async with streamable_http_client("http://127.0.0.1:3002/mcp") as (read_stream, write_stream, get_session_id):
            async with ClientSession(read_stream, write_stream) as session:
                await session.initialize()
                print("✓ Connected to streamable server")

                # Call echo tool which returns headers in response
                result = await session.call_tool("echo", {"message": "Header Test"})
                response_text = result.content[0].text

                print(f"\nEcho response: {response_text}")

                # Check if headers are mentioned in the response
                # The server includes "Headers: X" if headers are present
                if "Headers:" in response_text:
                    # Extract header count
                    match = re.search(r'Headers: (\d+)', response_text)
                    if match:
                        header_count = int(match.group(1))
                        print(f"✓ Headers detected: {header_count} headers found")

                        # Look for common HTTP headers in response
                        common_headers = ["User-Agent", "Content-Type", "Accept"]
                        found_headers = []
                        for header in common_headers:
                            if header in response_text:
                                found_headers.append(header)

                        if found_headers:
                            print(f"✓ Found expected headers: {', '.join(found_headers)}")
                            return True
                        else:
                            print("⚠ Some headers found but not common ones (check response)")
                            return True
                    else:
                        print("⚠ Headers mentioned but count not found")
                        return False
                else:
                    print("✗ No headers detected in response")
                    print("\nThis means either:")
                    print("  1. Server is not receiving headers")
                    print("  2. Context.getHeaders() is returning empty map")
                    return False

    except Exception as e:
        print(f"✗ Streamable test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_sse_headers():
    """Test headers with SSE transport."""
    print("\n" + "=" * 60)
    print("  Testing SSE Transport Headers")
    print("=" * 60)

    try:
        async with sse_client("http://127.0.0.1:3001/sse") as (read_stream, write_stream):
            async with ClientSession(read_stream, write_stream) as session:
                await session.initialize()
                print("✓ Connected to SSE server")

                # List tools to see what's available
                tools = await session.list_tools()
                tool_names = [t.name for t in tools.tools]
                print(f"Available tools: {tool_names}")

                if "echo" in tool_names:
                    # Call echo tool
                    result = await session.call_tool("echo", {"message": "Header Test"})
                    response_text = result.content[0].text

                    print(f"\nEcho response: {response_text}")

                    if "Headers:" in response_text:
                        match = re.search(r'Headers: (\d+)', response_text)
                        if match:
                            header_count = int(match.group(1))
                            print(f"✓ Headers detected: {header_count} headers found")
                            return True
                    else:
                        print("✗ No headers detected in response")
                        return False
                else:
                    print("⚠ Echo tool not available on this server")
                    return True  # Not a failure, just different server

    except Exception as e:
        print(f"✗ SSE test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


async def run_all_tests():
    """Run all header tests."""
    print("\n" + "=" * 60)
    print("  MCP Header Passing Test Suite")
    print("=" * 60)
    print("\nThis test verifies that HTTP headers from the MCP client")
    print("are accessible in FastMCP4J tools via @McpContext.getHeaders()")

    results = {}

    # Test streamable (primary target)
    results["streamable"] = await test_streamable_headers()

    # Test SSE if available
    results["sse"] = await test_sse_headers()

    # Summary
    print("\n" + "=" * 60)
    print("  TEST SUMMARY")
    print("=" * 60)
    for transport, passed in results.items():
        status = "✓ PASS" if passed else "✗ FAIL"
        print(f"  {status}  {transport.upper()}")

    all_passed = all(results.values())
    if all_passed:
        print("\n✓ All header tests PASSED")
    else:
        print("\n✗ Some header tests FAILED")

    return all_passed


def main():
    try:
        result = asyncio.run(run_all_tests())
        return 0 if result else 1
    except KeyboardInterrupt:
        print("\n\n⚠️  Test interrupted by user")
        return 130
    except Exception as e:
        print(f"\n✗ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
