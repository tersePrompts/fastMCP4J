#!/usr/bin/env python3
"""
Comprehensive MCP Test Suite - All Transports

Tests all FastMCP4J features across all transport types:
- Basic tools (echo, calculate)
- Async tools (@McpAsync)
- Built-in annotation tools (@McpMemory, @McpTodo, @McpPlanner, @McpFileRead, @McpFileWrite)
- Resources
- Context handling
- Progress reporting

Usage:
    python test_full_comprehensive.py streamable  # HTTP Streamable (port 3002)
    python test_full_comprehensive.py sse         # SSE (port 3001)
    python test_full_comprehensive.py stdio       # STDIO
    python test_full_comprehensive.py all         # Test all transports
"""

import asyncio
import sys
import subprocess
import os
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from mcp.client.sse import sse_client
from mcp.client.streamable_http import streamablehttp_client

# Test results tracking
TEST_RESULTS = {
    "passed": 0,
    "failed": 0,
    "errors": []
}

def log(msg):
    """Print log message with timestamp."""
    from datetime import datetime
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")

def print_header(title):
    """Print section header."""
    print("\n" + "=" * 60)
    print(f"  {title}")
    print("=" * 60)

def test_result(name, passed, details=""):
    """Record and print test result."""
    global TEST_RESULTS
    status = "[PASS]" if passed else "[FAIL]"
    print(f"{status} {name}")
    if details:
        print(f"      {details[:100]}")
    if passed:
        TEST_RESULTS["passed"] += 1
    else:
        TEST_RESULTS["failed"] += 1
        TEST_RESULTS["errors"].append((name, details))

async def test_tools(session):
    """Test all basic tools."""
    print_header("TOOL TESTS")

    # List tools
    tools = await session.list_tools()
    log(f"Found {len(tools.tools)} tools")
    for tool in tools.tools:
        log(f"  - {tool.name}: {tool.description[:50]}...")
    test_result("List tools", len(tools.tools) >= 2, f"Found {len(tools.tools)} tools")

    # Get tool names for flexible testing
    tool_names = {t.name for t in tools.tools}

    # Test echo
    try:
        result = await session.call_tool("echo", {"message": "Hello MCP!"})
        text = result.content[0].text if result.content else ""
        test_result("echo tool", "Hello MCP!" in text or "Echo" in text, text)
    except Exception as e:
        test_result("echo tool", False, str(e))

    # Test calculate (EchoServer) or add (SseFullServer/StdioFullServer)
    if "calculate" in tool_names:
        try:
            result = await session.call_tool("calculate", {"a": 15, "b": 27, "operation": "ADD"})
            text = result.content[0].text if result.content else ""
            test_result("calculate tool", "42" in text or "42.0" in text, text)
        except Exception as e:
            test_result("calculate tool", False, str(e))
    elif "add" in tool_names:
        try:
            result = await session.call_tool("add", {"a": 15, "b": 27})
            text = result.content[0].text if result.content else ""
            test_result("add tool", "42" in text or "42.0" in text, text)
        except Exception as e:
            test_result("add tool", False, str(e))

async def test_async_tools(session):
    """Test async tools (@McpAsync)."""
    print_header("ASYNC TOOL TESTS (@McpAsync)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "asyncTask" in tool_names:
        try:
            result = await session.call_tool("asyncTask", {"taskName": "test", "durationSeconds": 1})
            text = result.content[0].text if result.content else ""
            test_result("asyncTask tool", "completed" in text.lower() or "test" in text.lower(), text)
        except Exception as e:
            test_result("asyncTask tool", False, str(e))
    else:
        log("asyncTask tool not available on this server")

async def test_memory_tools(session):
    """Test @McpMemory tools."""
    print_header("MEMORY TOOL TESTS (@McpMemory)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "memory" not in tool_names:
        log("Memory tool not available on this server")
        return

    try:
        # Test memory write
        await session.call_tool("memory", {"mode": "write", "key": "test_key", "value": "test_value"})
        test_result("memory write", True)

        # Test memory read
        result = await session.call_tool("memory", {"mode": "read", "key": "test_key"})
        text = result.content[0].text if result.content else ""
        test_result("memory read", "test_value" in text, text)

        # Test memory list
        result = await session.call_tool("memory", {"mode": "list"})
        text = result.content[0].text if result.content else ""
        test_result("memory list", "test_key" in text or len(text) > 0, text[:100])

        # Test memory delete
        await session.call_tool("memory", {"mode": "delete", "key": "test_key"})
        test_result("memory delete", True)
    except Exception as e:
        test_result("memory tools", False, str(e))

async def test_todo_tools(session):
    """Test @McpTodo tools."""
    print_header("TODO TOOL TESTS (@McpTodo)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "todo" not in tool_names:
        log("Todo tool not available on this server")
        return

    try:
        # Test todo add
        await session.call_tool("todo", {"mode": "add", "id": "task1", "description": "Test task"})
        test_result("todo add", True)

        # Test todo list
        result = await session.call_tool("todo", {"mode": "list"})
        text = result.content[0].text if result.content else ""
        test_result("todo list", "task1" in text or "Test task" in text, text[:100])

        # Test todo update
        await session.call_tool("todo", {"mode": "update", "id": "task1", "status": "completed"})
        test_result("todo update", True)

        # Test todo delete
        await session.call_tool("todo", {"mode": "delete", "id": "task1"})
        test_result("todo delete", True)

        # Test todo clearCompleted
        await session.call_tool("todo", {"mode": "clearCompleted"})
        test_result("todo clearCompleted", True)
    except Exception as e:
        test_result("todo tools", False, str(e))

async def test_planner_tools(session):
    """Test @McpPlanner tools."""
    print_header("PLANNER TOOL TESTS (@McpPlanner)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "planner" not in tool_names:
        log("Planner tool not available on this server")
        return

    try:
        # Test planner createPlan
        result = await session.call_tool("planner", {"mode": "createPlan", "goal": "Test goal"})
        text = result.content[0].text if result.content else ""
        test_result("planner createPlan", "created" in text.lower() or "plan" in text.lower(), text[:100])

        # Test planner listPlans
        result = await session.call_tool("planner", {"mode": "listPlans"})
        text = result.content[0].text if result.content else ""
        test_result("planner listPlans", len(text) > 0, text[:100])
    except Exception as e:
        test_result("planner tools", False, str(e))

async def test_file_read_tools(session):
    """Test @McpFileRead tools."""
    print_header("FILE READ TOOL TESTS (@McpFileRead)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "fileread" not in tool_names:
        log("File read tool not available on this server")
        return

    # Create a test file first
    test_file = "test_mcp_read.txt"
    with open(test_file, "w") as f:
        f.write("Line 1\nLine 2\nLine 3\n")

    try:
        # Test readLines
        result = await session.call_tool("fileread", {"mode": "readLines", "path": test_file, "startLine": 1, "endLine": 2})
        text = result.content[0].text if result.content else ""
        test_result("fileread readLines", "Line 1" in text or "Line 2" in text, text[:100])

        # Test grep
        result = await session.call_tool("fileread", {"mode": "grep", "searchPath": test_file, "pattern": "Line 2"})
        text = result.content[0].text if result.content else ""
        test_result("fileread grep", "Line 2" in text, text[:100])

        # Test getStats
        result = await session.call_tool("fileread", {"mode": "getStats", "path": test_file})
        text = result.content[0].text if result.content else ""
        test_result("fileread getStats", "size" in text.lower() or "lines" in text.lower(), text[:100])
    except Exception as e:
        test_result("fileread tools", False, str(e))
    finally:
        # Clean up
        if os.path.exists(test_file):
            os.remove(test_file)

async def test_file_write_tools(session):
    """Test @McpFileWrite tools."""
    print_header("FILE WRITE TOOL TESTS (@McpFileWrite)")

    tools = await session.list_tools()
    tool_names = {t.name for t in tools.tools}

    if "filewrite" not in tool_names:
        log("File write tool not available on this server")
        return

    test_file = "test_mcp_write.txt"
    test_dir = "test_mcp_dir"

    try:
        # Test writeFile
        await session.call_tool("filewrite", {"mode": "writeFile", "path": test_file, "content": "Hello from MCP!"})
        with open(test_file, "r") as f:
            content = f.read()
        test_result("filewrite writeFile", "Hello from MCP!" in content)

        # Test appendFile
        await session.call_tool("filewrite", {"mode": "appendFile", "path": test_file, "content": "\nAppended line"})
        with open(test_file, "r") as f:
            content = f.read()
        test_result("filewrite appendFile", "Appended line" in content)

        # Test createDirectory
        await session.call_tool("filewrite", {"mode": "createDirectory", "path": test_dir})
        test_result("filewrite createDirectory", os.path.exists(test_dir))

        # Test deleteFile
        await session.call_tool("filewrite", {"mode": "deleteFile", "path": test_file})
        test_result("filewrite deleteFile", not os.path.exists(test_file))
    except Exception as e:
        test_result("filewrite tools", False, str(e))
    finally:
        # Clean up
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(test_dir):
            os.rmdir(test_dir)

async def test_resources(session):
    """Test resources."""
    print_header("RESOURCE TESTS")

    try:
        resources = await session.list_resources()
        log(f"Found {len(resources.resources)} resources")
        for resource in resources.resources:
            log(f"  - {resource.uri}: {resource.name}")
        test_result("List resources", len(resources.resources) >= 0, f"Found {len(resources.resources)} resources")

        # Try reading server://info if available
        if any(r.uri == "server://info" for r in resources.resources):
            result = await session.read_resource("server://info")
            if result.contents:
                text = result.contents[0].text if hasattr(result.contents[0], 'text') else str(result.contents[0])
                test_result("Read server://info", len(text) > 0, f"{len(text)} chars")
    except Exception as e:
        test_result("List resources", False, str(e))

async def test_prompts(session):
    """Test prompts."""
    print_header("PROMPT TESTS")

    try:
        prompts = await session.list_prompts()
        log(f"Found {len(prompts.prompts)} prompts")
        for prompt in prompts.prompts:
            log(f"  - {prompt.name}: {prompt.description}")
        test_result("List prompts", True, f"Found {len(prompts.prompts)} prompts")
    except Exception as e:
        test_result("List prompts", False, str(e))

async def run_tests(transport_type):
    """Run tests for a specific transport type."""
    global TEST_RESULTS
    TEST_RESULTS = {"passed": 0, "failed": 0, "errors": []}

    print_header(f"TESTING TRANSPORT: {transport_type.upper()}")

    try:
        if transport_type == "streamable":
            url = "http://localhost:3002/mcp"
            async with streamablehttp_client(url) as (read, write, _):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    await run_all_tests(session)
        elif transport_type == "sse":
            url = "http://localhost:3001/sse"
            async with sse_client(url) as (read, write):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    await run_all_tests(session)
        elif transport_type == "stdio":
            server_params = StdioServerParameters(
                command="mvn",
                args=["-q", "exec:java",
                      "-Dexec.mainClass=com.ultrathink.fastmcp.example.StdioFullServer",
                      "-Dexec.classpathScope=test"],
            )
            async with stdio_client(server_params) as (read, write):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    await run_all_tests(session)
    except Exception as e:
        log(f"Failed to connect to {transport_type} server: {e}")
        test_result(f"Connect to {transport_type}", False, str(e))

async def run_all_tests(session):
    """Run all test categories."""
    await test_tools(session)
    await test_async_tools(session)
    await test_memory_tools(session)
    await test_todo_tools(session)
    await test_planner_tools(session)
    await test_file_read_tools(session)
    await test_file_write_tools(session)
    await test_resources(session)
    await test_prompts(session)

def print_summary(transport_type):
    """Print test summary."""
    print_header(f"TEST SUMMARY: {transport_type.upper()}")
    total = TEST_RESULTS["passed"] + TEST_RESULTS["failed"]
    pass_rate = (TEST_RESULTS["passed"] / total * 100) if total > 0 else 0
    print(f"Total Tests: {total}")
    print(f"Passed: {TEST_RESULTS['passed']}")
    print(f"Failed: {TEST_RESULTS['failed']}")
    print(f"Pass Rate: {pass_rate:.1f}%")

    if TEST_RESULTS["errors"]:
        print("\nFailed Tests:")
        for name, details in TEST_RESULTS["errors"]:
            print(f"  - {name}: {details[:80]}")

async def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: python test_full_comprehensive.py <transport>")
        print("Transports: streamable, sse, stdio, all")
        sys.exit(1)

    transports = sys.argv[1].lower()
    if transports == "all":
        for transport in ["streamable", "sse", "stdio"]:
            await run_tests(transport)
            print_summary(transport)
            await asyncio.sleep(2)
    else:
        await run_tests(transports)
        print_summary(transports)

if __name__ == "__main__":
    asyncio.run(main())
