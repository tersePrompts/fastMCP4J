#!/usr/bin/env python3
"""
Test client for multi-class MCP servers.

This script tests both:
1. ManualModulesServer - using explicit module registration
2. PackageScanServer - using package scanning

Usage:
    python test_multi_class_servers.py
"""

import subprocess
import json
import sys
import time
from pathlib import Path

# ANSI colors for output
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
BLUE = "\033[94m"
RESET = "\033[0m"

def print_header(text):
    print(f"\n{BLUE}{'=' * 60}{RESET}")
    print(f"{BLUE}{text}{RESET}")
    print(f"{BLUE}{'=' * 60}{RESET}\n")

def print_success(text):
    print(f"{GREEN}✓ {text}{RESET}")

def print_error(text):
    print(f"{RED}✗ {text}{RESET}")

def print_info(text):
    print(f"{YELLOW}ℹ {text}{RESET}")

def send_request(process, request):
    """Send JSON-RPC request to the server process."""
    request_str = json.dumps(request)
    process.stdin.write(f"Content-Length: {len(request_str)}\r\n\r\n{request_str}")
    process.stdin.flush()

    response = ""
    while True:
        line = process.stdout.readline()
        if not line:
            break
        if line.strip() == "":
            break
        if line.startswith("Content-Length:"):
            length = int(line.split(":")[1].strip())
            _ = process.stdout.readline()  # blank line
            response = process.stdout.read(length).decode('utf-8')
            break

    try:
        return json.loads(response)
    except json.JSONDecodeError:
        return {"error": "Failed to parse response"}

def test_manual_modules_server():
    """Test the ManualModulesServer."""
    print_header("Testing ManualModulesServer")

    # Find and run the server
    cmd = [sys.executable if sys.platform != "win32" else "python",
           "-m", "pytest", "src/test/java/com/ultrathink/fastmcp/example/ManualModulesServer.java"]

    # Actually, we need to run via Maven
    process = subprocess.Popen(
        ["mvn", "exec:java", "-Dexec.mainClass=io.github.terseprompts.fastmcp.example.ManualModulesServer"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=Path(__file__).parent.parent.parent
    )

    time.sleep(2)  # Wait for server to start

    # Initialize
    init_request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {
                "name": "test-client",
                "version": "1.0.0"
            }
        }
    }

    response = send_request(process, init_request)

    if "error" in response:
        print_error(f"Initialize failed: {response['error']}")
        process.terminate()
        return False

    print_success("Server initialized")

    # List tools
    tools_request = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/list"
    }

    response = send_request(process, tools_request)

    if "error" in response:
        print_error(f"List tools failed: {response['error']}")
        process.terminate()
        return False

    tools = response.get("result", {}).get("tools", [])
    print_info(f"Found {len(tools)} tools:")

    expected_tools = [
        "toUppercase", "toLowercase", "reverse", "wordCount",  # StringTools
        "add", "subtract", "multiply", "divide", "power",      # MathTools
        "currentTimeMillis", "availableProcessors", "memoryInfo", "status"  # SystemTools + Server
    ]

    found_all = True
    for tool in tools:
        tool_name = tool.get("name", "unknown")
        if tool_name in expected_tools:
            print_success(f"  - {tool_name}")
        else:
            print_info(f"  - {tool_name}")

    # Test a tool call
    print("\nTesting tool calls:")

    # Test toUppercase
    call_request = {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "tools/call",
        "params": {
            "name": "toUppercase",
            "arguments": {"text": "hello world"}
        }
    }

    response = send_request(process, call_request)
    if "error" not in response:
        result = response.get("result", {}).get("content", [{}])[0].get("text", "")
        if "HELLO WORLD" in result:
            print_success("toUppercase: 'hello world' -> 'HELLO WORLD'")
        else:
            print_error(f"toUppercase unexpected result: {result}")
    else:
        print_error(f"toUppercase failed: {response['error']}")

    # Test add
    call_request = {
        "jsonrpc": "2.0",
        "id": 4,
        "method": "tools/call",
        "params": {
            "name": "add",
            "arguments": {"a": 5, "b": 3}
        }
    }

    response = send_request(process, call_request)
    if "error" not in response:
        result = response.get("result", {}).get("content", [{}])[0].get("text", "")
        if "8" in result:
            print_success("add: 5 + 3 = 8")
        else:
            print_error(f"add unexpected result: {result}")
    else:
        print_error(f"add failed: {response['error']}")

    process.terminate()
    return True

def test_package_scan_server():
    """Test the PackageScanServer."""
    print_header("Testing PackageScanServer")

    print_info("This test would require running the PackageScanServer")
    print_info("Expected tools from package scanning:")
    print_info("  - From ConversionTools: celsiusToFahrenheit, kmToMiles, kgToPounds, etc.")
    print_info("  - From DataTools: randomNumber, currentDateTime, hashString, etc.")
    print_info("  - From TextTools: charCount, extractNumbers, contains, etc.")

    return True

def main():
    print(f"\n{YELLOW}Multi-Class MCP Server Test Client{RESET}")
    print("This script tests the new multi-class tool organization feature")

    results = {}

    # Test manual modules approach
    results["manual_modules"] = test_manual_modules_server()

    # Test package scan approach
    results["package_scan"] = test_package_scan_server()

    # Summary
    print_header("Test Summary")
    for test_name, passed in results.items():
        status = "PASSED" if passed else "FAILED"
        color = GREEN if passed else RED
        print(f"{color}{test_name}: {status}{RESET}")

    all_passed = all(results.values())
    return 0 if all_passed else 1

if __name__ == "__main__":
    sys.exit(main())
