#!/usr/bin/env python3
import asyncio
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

async def main():
    url = "http://localhost:3002/mcp"
    async with streamablehttp_client(url) as (read, write, _):
        async with ClientSession(read, write) as session:
            await session.initialize()
            print("[OK] Connected")

            tools = await session.list_tools()
            print(f"[TOOLS] {len(tools.tools)}")

            result = await session.call_tool("echo", {"message": "test"})
            print(f"[ECHO] {result.content[0].text}")

            result = await session.call_tool("calculate", {"a": 10, "operation": "ADD", "b": 5})
            print(f"[CALC] 10 + 5 = {result.content[0].text}")

            print("[OK] All tests passed!")

if __name__ == "__main__":
    asyncio.run(main())
