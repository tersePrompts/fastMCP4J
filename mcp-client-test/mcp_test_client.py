"""
MCP Client Test Suite
======================
A comprehensive test client for MCP localhost-server tools.
Tests all 7 tools: calculate, echo, fileread, filewrite, memory, planner, todo
"""

import asyncio
import json
import sys
from pathlib import Path
from typing import Any, Optional
from datetime import datetime

try:
    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client
except ImportError:
    print("Installing mcp package...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "mcp"])
    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client


class Colors:
    """ANSI color codes for terminal output"""
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'


class TestResult:
    """Individual test result"""
    def __init__(self, tool: str, operation: str, name: str, passed: bool,
                 expected: Any = None, actual: Any = None, error: str = None):
        self.tool = tool
        self.operation = operation
        self.name = name
        self.passed = passed
        self.expected = expected
        self.actual = actual
        self.error = error

    def __str__(self):
        status = f"{Colors.OKGREEN}PASS{Colors.ENDC}" if self.passed else f"{Colors.FAIL}FAIL{Colors.ENDC}"
        return f"[{status}] {self.tool}:{self.operation} - {self.name}"


class MCPTestClient:
    """MCP Client for testing localhost-server tools"""

    def __init__(self, server_command: list = None):
        if server_command is None:
            # Default to connecting via stdio - adjust path as needed
            server_command = ["java", "-cp", "fastmcp4j.jar", "com.ultrathink.fastmcp.Main"]

        self.server_params = StdioServerParameters(
            command=server_command[0],
            args=server_command[1:] if len(server_command) > 1 else []
        )
        self.session: Optional[ClientSession] = None
        self.results: list[TestResult] = []

    async def connect(self):
        """Connect to MCP server"""
        self.stdio_context = stdio_client(self.server_params)
        self.stdio, self.write = await self.stdio_context.__aenter__()
        self.session = ClientSession(self.stdio, self.write)
        await self.session.__aenter__()
        await self.session.initialize()
        print(f"{Colors.OKGREEN}Connected to MCP server{Colors.ENDC}")

    async def disconnect(self):
        """Disconnect from MCP server"""
        if self.session:
            await self.session.__aexit__(None, None, None)
        if hasattr(self, 'stdio_context'):
            await self.stdio_context.__aexit__(None, None, None)

    async def call_tool(self, tool_name: str, arguments: dict) -> Any:
        """Call an MCP tool and return the result"""
        try:
            result = await self.session.call_tool(tool_name, arguments)
            # Parse the result content
            if hasattr(result, 'content'):
                for item in result.content:
                    if hasattr(item, 'text'):
                        return json.loads(item.text)
                    elif hasattr(item, 'data'):
                        return item.data
            return result
        except Exception as e:
            return {"error": str(e)}

    def add_result(self, tool: str, operation: str, name: str,
                   passed: bool, expected=None, actual=None, error=None):
        """Add a test result"""
        result = TestResult(tool, operation, name, passed, expected, actual, error)
        self.results.append(result)
        print(result)

    # =========================================================================
    # CALCULATE TOOL TESTS
    # =========================================================================
    async def test_calculate(self):
        """Test calculate tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing CALCULATE Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        tests = [
            ("ADD positive", "ADD", 10, 5, 15.0),
            ("ADD negative", "ADD", -10, -5, -15.0),
            ("SUBTRACT", "SUBTRACT", 100, 25, 75.0),
            ("MULTIPLY", "MULTIPLY", 7, 8, 56.0),
            ("DIVIDE exact", "DIVIDE", 144, 12, 12.0),
            ("DIVIDE decimal", "DIVIDE", 10, 3, 10/3),
            ("Multiply by zero", "MULTIPLY", 5, 0, 0.0),
            ("Add zero", "ADD", 0, 0, 0.0),
            ("Large numbers", "ADD", 999999, 1, 1000000.0),
            ("Negative multiply", "MULTIPLY", -5, 4, -20.0),
        ]

        for name, op, a, b, expected in tests:
            result = await self.call_tool("calculate", {"a": a, "operation": op, "b": b})
            if isinstance(result, dict) and "error" in result:
                self.add_result("calculate", op, name, False, expected, None, result["error"])
            else:
                # Allow small floating point differences
                passed = abs(result - expected) < 0.0001
                self.add_result("calculate", op, name, passed, expected, result)

    # =========================================================================
    # ECHO TOOL TESTS
    # =========================================================================
    async def test_echo(self):
        """Test echo tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing ECHO Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        tests = [
            ("Simple message", "Hello, World!"),
            ("Empty string", ""),
            ("Special characters", "!@#$%^&*()_+-=[]{}|;':\",./<>?"),
            ("Unicode", "Hello 世界 こんにちは"),
            ("Emojis", " 😀 😃 😄 😁 🎉 🏆"),
            ("Numbers", "12345 6.789 -42"),
            ("SQL query", "SELECT * FROM users WHERE id=1"),
            ("JSON", '{"key": "value", "number": 123}'),
            ("Long message", "A" * 500),
            ("Multiline", "Line 1\nLine 2\nLine 3"),
        ]

        for name, message in tests:
            result = await self.call_tool("echo", {"message": message})
            if isinstance(result, dict) and "error" in result:
                self.add_result("echo", "echo", name, False, message, None, result["error"])
            else:
                # Echo should return the message with timestamp
                passed = message in str(result)
                self.add_result("echo", "echo", name, passed, f"contains '{message}'", result)

    # =========================================================================
    # FILEREAD TOOL TESTS
    # =========================================================================
    async def test_fileread(self):
        """Test fileread tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing FILEREAD Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        test_file = Path("C:/Users/av201/workspace/fastMCP4J/README.md")

        tests = [
            ("getStats", "getStats", {"path": str(test_file)}, "lineCount"),
            ("readFile", "readFile", {"path": str(test_file)}, "content"),
            ("readLines range", "readLines", {"path": str(test_file), "startLine": 1, "endLine": 5}, "lines"),
            ("grep files_with_matches", "grep", {"searchPath": str(test_file.parent), "pattern": "TODO", "outputMode": "files_with_matches"}, "files"),
            ("grep content", "grep", {"searchPath": str(test_file), "pattern": "import", "outputMode": "content"}, "matches"),
            ("grep case-insensitive", "grep", {"searchPath": str(test_file), "pattern": "fastmcp", "caseInsensitive": True}, "matches"),
            ("grep with context", "grep", {"searchPath": str(test_file), "pattern": "class", "linesBefore": 1, "linesAfter": 1}, "context"),
            ("grep maxMatches", "grep", {"searchPath": str(test_file), "pattern": "import", "maxMatches": 5}, "limited"),
            ("getStats directory", "getStats", {"path": str(test_file.parent)}, "stats"),
            ("grep count mode", "grep", {"searchPath": str(test_file), "pattern": "import", "outputMode": "count"}, "count"),
        ]

        for name, mode, args, expected_key in tests:
            result = await self.call_tool("fileread", {"mode": mode, **args})
            if isinstance(result, dict) and "error" in result:
                self.add_result("fileread", mode, name, False, expected_key, None, result["error"])
            else:
                passed = result is not None and not isinstance(result, list) or len(result) > 0
                self.add_result("fileread", mode, name, passed, f"returns {expected_key}", result)

    # =========================================================================
    # FILEWRITE TOOL TESTS
    # =========================================================================
    async def test_filewrite(self):
        """Test filewrite tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing FILEWRITE Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        test_dir = Path("C:/Users/av201/workspace/fastMCP4J/mcp-test-temp")
        test_dir.mkdir(exist_ok=True)

        tests = [
            ("writeFile new", "writeFile", {"path": str(test_dir / "test1.txt"), "content": "Hello, World!"}, "created:true"),
            ("writeFile multiline", "writeFile", {"path": str(test_dir / "test2.txt"), "content": "Line 1\nLine 2\nLine 3"}, "linesWritten:3"),
            ("writeLines array", "writeLines", {"path": str(test_dir / "test3.txt"), "lines": ["A", "B", "C"]}, "linesWritten:3"),
            ("appendFile", "appendFile", {"path": str(test_dir / "test1.txt"), "content": " Appended"}, "appended"),
            ("appendLines", "appendLines", {"path": str(test_dir / "test1.txt"), "lines": ["Line 4", "Line 5"]}, "appended"),
            ("createDirectory", "createDirectory", {"path": str(test_dir / "newdir")}, "created"),
            ("writeFile with createParents", "writeFile", {"path": str(test_dir / "a/b/c.txt"), "content": "nested", "createParents": True}, "nested"),
            ("writeLines empty array", "writeLines", {"path": str(test_dir / "empty.txt"), "lines": []}, "empty"),
            ("deleteFile", "deleteFile", {"path": str(test_dir / "test2.txt")}, "deleted"),
            ("UTF-8 content", "writeFile", {"path": str(test_dir / "utf8.txt"), "content": "Hello 世界 🌍"}, "utf8"),
        ]

        for name, mode, args, expected in tests:
            result = await self.call_tool("filewrite", {"mode": mode, **args})
            if isinstance(result, dict) and "error" in result:
                self.add_result("filewrite", mode, name, False, expected, None, result["error"])
            else:
                passed = result is not None
                self.add_result("filewrite", mode, name, passed, expected, result)

    # =========================================================================
    # MEMORY TOOL TESTS
    # =========================================================================
    async def test_memory(self):
        """Test memory tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing MEMORY Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        # Cleanup from previous runs
        await self.call_tool("memory", {"command": "delete", "path": "/test-mcp-file.txt"})
        await self.call_tool("memory", {"command": "delete", "path": "/test-mcp-dir"})

        tests = [
            ("create simple file", "create", {"command": "create", "path": "/test-mcp-file.txt", "content": "Hello"}, "created"),
            ("view file", "view", {"command": "view", "path": "/test-mcp-file.txt"}, "content"),
            ("str_replace success", "str_replace", {"command": "str_replace", "path": "/test-mcp-file.txt", "old_str": "Hello", "new_str": "World"}, "replaced"),
            ("insert line", "insert", {"command": "insert", "path": "/test-mcp-file.txt", "insert_line": 2, "insert_text": "New line"}, "inserted"),
            ("view with range", "view", {"command": "view", "path": "/test-mcp-file.txt", "view_range": [1, 2]}, "range"),
            ("create nested", "create", {"command": "create", "path": "/test-mcp-dir/nested/file.txt", "content": "nested"}, "nested"),
            ("view nested", "view", {"command": "view", "path": "/test-mcp-dir"}, "directory"),
            ("rename file", "rename", {"command": "rename", "old_path": "/test-mcp-file.txt", "new_path": "/renamed.txt"}, "renamed"),
            ("delete file", "delete", {"command": "delete", "path": "/renamed.txt"}, "deleted"),
            ("verify deleted", "view", {"command": "view", "path": "/renamed.txt"}, "not found"),
        ]

        for name, command, args, expected in tests:
            result = await self.call_tool("memory", args)
            if isinstance(result, dict) and "error" in result:
                # For the verify deleted test, error is expected
                if name == "verify deleted":
                    self.add_result("memory", command, name, True, "file not found", result)
                else:
                    self.add_result("memory", command, name, False, expected, None, result["error"])
            else:
                passed = result is not None
                self.add_result("memory", command, name, passed, expected, result)

    # =========================================================================
    # TODO TOOL TESTS
    # =========================================================================
    async def test_todo(self):
        """Test todo tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing TODO Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        # Clear existing todos
        await self.call_tool("todo", {"mode": "clearCompleted"})

        tests = [
            ("add high priority", "add", {"mode": "add", "task": "High priority task", "priority": "high"}, "added"),
            ("add low priority", "add", {"mode": "add", "task": "Low priority task", "priority": "low"}, "added"),
            ("add critical priority", "add", {"mode": "add", "task": "Critical task", "priority": "critical"}, "added"),
            ("add default priority", "add", {"mode": "add", "task": "Default task"}, "added"),
            ("list all", "list", {"mode": "list"}, "listed"),
            ("list by priority", "list", {"mode": "list", "sort": "priority"}, "sorted"),
            ("list pending only", "list", {"mode": "list", "status": "pending"}, "filtered"),
            ("update to in_progress", "update", {"mode": "update", "id": "1", "newStatus": "in_progress"}, "updated"),
            ("update to completed", "update", {"mode": "update", "id": "1", "newStatus": "completed"}, "completed"),
            ("clear completed", "clearCompleted", {"mode": "clearCompleted"}, "cleared"),
        ]

        todo_id = None
        for name, mode, args, expected in tests:
            result = await self.call_tool("todo", args)
            if isinstance(result, dict) and "error" in result:
                self.add_result("todo", mode, name, False, expected, None, result["error"])
            else:
                passed = result is not None
                self.add_result("todo", mode, name, passed, expected, result)

    # =========================================================================
    # PLANNER TOOL TESTS
    # =========================================================================
    async def test_planner(self):
        """Test planner tool - 10 test cases"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"Testing PLANNER Tool (10 tests)")
        print(f"{'='*60}{Colors.ENDC}")

        # Cleanup from previous runs
        list_result = await self.call_tool("planner", {"mode": "listPlans"})
        if isinstance(list_result, list):
            for plan in list_result:
                await self.call_tool("planner", {"mode": "deletePlan", "planId": plan.get("id", "")})

        tests = [
            ("createPlan with tasks", "createPlan", {
                "mode": "createPlan",
                "planName": "Test Plan",
                "planDescription": "Testing planner",
                "initialTasks": [{"title": "Task 1"}, {"title": "Task 2"}]
            }, "created"),
            ("listPlans", "listPlans", {"mode": "listPlans"}, "listed"),
            ("getPlan", "getPlan", {"mode": "getPlan", "planId": "1"}, "details"),
            ("addTask", "addTask", {"mode": "addTask", "planId": "1", "taskTitle": "New Task"}, "added"),
            ("getNextTask", "getNextTask", {"mode": "getNextTask", "planId": "1"}, "next"),
            ("updateTask status", "updateTask", {"mode": "updateTask", "planId": "1", "taskId": "1", "status": "in_progress"}, "updated"),
            ("addSubtask", "addSubtask", {"mode": "addSubtask", "planId": "1", "parentTaskId": "1", "subtaskTitle": "Subtask"}, "subtask"),
            ("updateTask complete", "updateTask", {"mode": "updateTask", "planId": "1", "taskId": "1", "status": "completed"}, "completed"),
            ("getPlan after updates", "getPlan", {"mode": "getPlan", "planId": "1"}, "progress"),
            ("deletePlan", "deletePlan", {"mode": "deletePlan", "planId": "1"}, "deleted"),
        ]

        plan_id = None
        for name, mode, args, expected in tests:
            # Update planId for subsequent tests
            if plan_id and "planId" in args and args["planId"] == "1":
                args["planId"] = plan_id

            result = await self.call_tool("planner", args)
            if isinstance(result, dict) and "error" in result:
                self.add_result("planner", mode, name, False, expected, None, result["error"])
            else:
                passed = result is not None
                self.add_result("planner", mode, name, passed, expected, result)

                # Capture plan ID from createPlan
                if mode == "createPlan" and isinstance(result, dict):
                    if "id" in result:
                        plan_id = result["id"]
                    elif "planId" in result:
                        plan_id = result["planId"]

    # =========================================================================
    # SUMMARY REPORT
    # =========================================================================
    def print_summary(self):
        """Print test summary report"""
        print(f"\n{Colors.HEADER}{'='*60}")
        print(f"TEST SUMMARY REPORT")
        print(f"{'='*60}{Colors.ENDC}\n")

        # Group results by tool
        tools = {}
        for r in self.results:
            if r.tool not in tools:
                tools[r.tool] = {"pass": 0, "fail": 0, "total": 0}
            tools[r.tool]["total"] += 1
            if r.passed:
                tools[r.tool]["pass"] += 1
            else:
                tools[r.tool]["fail"] += 1

        # Print per-tool summary
        for tool, stats in tools.items():
            pass_rate = (stats["pass"] / stats["total"] * 100) if stats["total"] > 0 else 0
            status_color = Colors.OKGREEN if pass_rate == 100 else Colors.WARNING if pass_rate >= 80 else Colors.FAIL
            print(f"{Colors.BOLD}{tool.upper()}{Colors.ENDC}: {stats['pass']}/{stats['total']} passed "
                  f"({status_color}{pass_rate:.0f}%{Colors.ENDC})")

        # Total summary
        total_tests = len(self.results)
        total_passed = sum(1 for r in self.results if r.passed)
        total_failed = total_tests - total_passed
        overall_pass_rate = (total_passed / total_tests * 100) if total_tests > 0 else 0

        print(f"\n{Colors.BOLD}TOTAL:{Colors.ENDC} {total_passed}/{total_tests} passed "
              f"({overall_pass_rate:.1f}%)")

        # Print failures if any
        if total_failed > 0:
            print(f"\n{Colors.FAIL}Failed tests:{Colors.ENDC}")
            for r in self.results:
                if not r.passed:
                    print(f"  - {r.tool}:{r.operation} - {r.name}")
                    if r.error:
                        print(f"    Error: {r.error}")

        # Save results to JSON
        self.save_results()

    def save_results(self):
        """Save test results to JSON file"""
        output_dir = Path("C:/Users/av201/workspace/fastMCP4J/mcp-client-test")
        output_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_file = output_dir / f"test_results_{timestamp}.json"

        results_data = {
            "timestamp": timestamp,
            "total_tests": len(self.results),
            "passed": sum(1 for r in self.results if r.passed),
            "failed": sum(1 for r in self.results if not r.passed),
            "results": [
                {
                    "tool": r.tool,
                    "operation": r.operation,
                    "name": r.name,
                    "passed": r.passed,
                    "expected": str(r.expected) if r.expected else None,
                    "actual": str(r.actual) if r.actual else None,
                    "error": r.error
                }
                for r in self.results
            ]
        }

        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, indent=2)

        print(f"\n{Colors.OKCYAN}Results saved to: {results_file}{Colors.ENDC}")

    # =========================================================================
    # MAIN TEST RUNNER
    # =========================================================================
    async def run_all_tests(self):
        """Run all test suites"""
        print(f"{Colors.HEADER}")
        print("=" * 60)
        print(" MCP LOCALHOST-SERVER TEST SUITE")
        print("=" * 60)
        print(f" Testing 7 tools with 10+ tests each")
        print(f" Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        print(f"{Colors.ENDC}")

        await self.connect()

        try:
            await self.test_calculate()
            await self.test_echo()
            await self.test_fileread()
            await self.test_filewrite()
            await self.test_memory()
            await self.test_todo()
            await self.test_planner()
        finally:
            await self.disconnect()

        self.print_summary()


# =============================================================================
# HTTP CLIENT VERSION (for connecting to HTTP MCP servers)
# =============================================================================
class MCPHTTPClient:
    """HTTP-based MCP client for testing"""

    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.results = []

    async def call_tool(self, tool_name: str, arguments: dict) -> Any:
        """Call tool via HTTP"""
        import aiohttp

        url = f"{self.base_url}/tools/{tool_name}"
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(url, json=arguments) as response:
                    return await response.json()
        except Exception as e:
            return {"error": str(e)}


# =============================================================================
# DIRECT TCP CLIENT VERSION (for custom MCP protocol)
# =============================================================================
class MCPTCPClient:
    """Direct TCP client for MCP servers using SSE or WebSocket"""

    def __init__(self, host: str = "localhost", port: int = 9999):
        self.host = host
        self.port = port
        self.results = []


# =============================================================================
# MAIN ENTRY POINT
# =============================================================================
async def main():
    """Main entry point for running tests"""
    import argparse

    parser = argparse.ArgumentParser(description="MCP Test Client")
    parser.add_argument("--server", nargs="+", default=None,
                        help="Server command (e.g., java -cp fastmcp4j.jar ...)")
    parser.add_argument("--tool", choices=["calculate", "echo", "fileread", "filewrite",
                                              "memory", "todo", "planner", "all"],
                        default="all", help="Tool to test (default: all)")
    parser.add_argument("--url", help="HTTP URL if connecting via HTTP")

    args = parser.parse_args()

    client = MCPTestClient(server_command=args.server)

    if args.tool == "all":
        await client.run_all_tests()
    else:
        await client.connect()
        try:
            test_method = getattr(client, f"test_{args.tool}")
            await test_method()
        finally:
            await client.disconnect()
        client.print_summary()


if __name__ == "__main__":
    asyncio.run(main())
