#!/usr/bin/env python3
"""Quick test for Streamable HTTP transport (port 3002)."""
import asyncio
import sys
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

async def main():
    url = "http://localhost:3002/mcp"
    try:
        async with streamablehttp_client(url) as (read, write, _):
            async with ClientSession(read, write) as session:
                await session.initialize()
                print("[OK] Connected to Streamable HTTP server")

                tools = await session.list_tools()
                print(f"[TOOLS] {len(tools.tools)} tools")
                for t in tools.tools:
                    print(f"  - {t.name}")

                # Test calculate
                result = await session.call_tool("calculate", {"a": 10, "b": 5, "operation": "ADD"})
                print(f"[CALC] 10 + 5 = {result.content[0].text}")

                # Test memory
                await session.call_tool("memory", {"mode": "write", "key": "test", "value": "Hello"})
                result = await session.call_tool("memory", {"mode": "read", "key": "test"})
                print(f"[MEMORY] {result.content[0].text}")

                print("[OK] All tests passed!")
                return 0
    except Exception as e:
        print(f"[ERROR] Test failed: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
