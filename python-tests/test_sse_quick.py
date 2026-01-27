#!/usr/bin/env python3
"""Quick test for SSE transport (port 3001)."""
import asyncio
import sys
import time
from mcp import ClientSession
from mcp.client.sse import sse_client

async def main():
    url = "http://localhost:3001/sse"
    max_retries = 5
    retry_delay = 2

    for attempt in range(max_retries):
        try:
            print(f"[INFO] Attempt {attempt + 1}/{max_retries}: Connecting to {url}")
            async with sse_client(url) as (read, write):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    print("[OK] Connected to SSE server")

                    tools = await session.list_tools()
                    print(f"[TOOLS] {len(tools.tools)} tools")
                    for t in tools.tools:
                        print(f"  - {t.name}")

                    # Test add
                    result = await session.call_tool("add", {"a": 10, "b": 5})
                    print(f"[ADD] 10 + 5 = {result.content[0].text}")

                    # Test asyncTask
                    result = await session.call_tool("asyncTask", {"taskName": "test"})
                    print(f"[ASYNC] {result.content[0].text}")

                    # Test memory
                    await session.call_tool("memory", {"mode": "write", "key": "test", "value": "Hello"})
                    result = await session.call_tool("memory", {"mode": "read", "key": "test"})
                    print(f"[MEMORY] {result.content[0].text}")

                    print("[OK] All tests passed!")
                    return 0
        except Exception as e:
            print(f"[ERROR] Attempt {attempt + 1} failed: {e}", file=sys.stderr)
            if attempt < max_retries - 1:
                print(f"[INFO] Retrying in {retry_delay} seconds...")
                await asyncio.sleep(retry_delay)
            else:
                print("[ERROR] Max retries exceeded", file=sys.stderr)
                import traceback
                traceback.print_exc()
                return 1

if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
