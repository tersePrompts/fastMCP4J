#!/usr/bin/env python3
"""Quick test for STDIO transport."""
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main():
    server_params = StdioServerParameters(
        command="mvn",
        args=["-q", "exec:java",
              "-Dexec.mainClass=com.ultrathink.fastmcp.example.StdioFullServer",
              "-Dexec.classpathScope=test"],
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            print("[OK] Connected to STDIO server")

            tools = await session.list_tools()
            print(f"[TOOLS] {len(tools.tools)} tools")
            for t in tools.tools:
                print(f"  - {t.name}")

            # Test add
            result = await session.call_tool("add", {"a": 10, "b": 5})
            print(f"[ADD] 10 + 5 = {result.content[0].text}")

            # Test asyncTask
            result = await session.call_tool("asyncTask", {"taskName": "test", "durationSeconds": 1})
            print(f"[ASYNC] {result.content[0].text}")

            # Test memory
            await session.call_tool("memory", {"mode": "write", "key": "test", "value": "Hello"})
            result = await session.call_tool("memory", {"mode": "read", "key": "test"})
            print(f"[MEMORY] {result.content[0].text}")

            print("[OK] All tests passed!")

if __name__ == "__main__":
    asyncio.run(main())
