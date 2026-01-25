"""
Simple MCP Test Client - Synchronous Version
=============================================
A simplified synchronous test client for MCP tools.
Uses requests library for HTTP communication.
"""

import json
import sys
from pathlib import Path
from datetime import datetime
from typing import Any, Dict, List

try:
    import requests
except ImportError:
    print("Installing requests...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "requests"])
    import requests


class Colors:
    """ANSI color codes"""
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'


class SimpleMCPClient:
    """Simple HTTP-based MCP client"""

    def __init__(self, base_url: str = "http://localhost:8080/mcp"):
        self.base_url = base_url
        self.results: List[Dict] = []

    def call_tool(self, tool_name: str, arguments: Dict) -> Any:
        """Call an MCP tool via HTTP POST"""
        url = f"{self.base_url}/tools/{tool_name}"
        try:
            response = requests.post(url, json=arguments, timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            return {"error": str(e)}

    def add_result(self, tool: str, op: str, name: str, passed: bool,
                   expected: Any = None, actual: Any = None, error: str = None):
        """Record test result"""
        self.results.append({
            "tool": tool,
            "operation": op,
            "name": name,
            "passed": passed,
            "expected": str(expected) if expected else None,
            "actual": str(actual) if actual else None,
            "error": error
        })
        status = f"{Colors.OKGREEN}PASS{Colors.ENDC}" if passed else f"{Colors.FAIL}FAIL{Colors.ENDC}"
        print(f"[{status}] {tool}:{op} - {name}")

    def print_summary(self):
        """Print test summary"""
        total = len(self.results)
        passed = sum(1 for r in self.results if r["passed"])
        failed = total - passed

        print(f"\n{'='*50}")
        print(f"SUMMARY: {passed}/{total} passed ({passed*100//total}%)")
        print(f"{'='*50}")

        # Group by tool
        by_tool = {}
        for r in self.results:
            t = r["tool"]
            if t not in by_tool:
                by_tool[t] = {"pass": 0, "fail": 0}
            if r["passed"]:
                by_tool[t]["pass"] += 1
            else:
                by_tool[t]["fail"] += 1

        for tool, stats in by_tool.items():
            print(f"{tool.upper()}: {stats['pass']}/{stats['pass']+stats['fail']} passed")

        if failed > 0:
            print(f"\n{Colors.FAIL}Failed:{Colors.ENDC}")
            for r in self.results:
                if not r["passed"]:
                    print(f"  - {r['tool']}: {r['name']}")
                    if r.get("error"):
                        print(f"    {r['error']}")

    def save_results(self):
        """Save results to JSON"""
        out_dir = Path(__file__).parent
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_file = out_dir / f"results_{timestamp}.json"

        with open(results_file, 'w') as f:
            json.dump({
                "timestamp": timestamp,
                "total": len(self.results),
                "passed": sum(1 for r in self.results if r["passed"]),
                "failed": sum(1 for r in self.results if not r["passed"]),
                "results": self.results
            }, f, indent=2)

        print(f"\nResults saved to: {results_file}")

    # ========================================================================
    # TEST METHODS
    # ========================================================================

    def test_calculate(self):
        """Test calculate tool"""
        print(f"\n{'='*50}\nTesting CALCULATE\n{'='*50}")

        tests = [
            ("ADD positive", "ADD", 10, 5, 15.0),
            ("SUBTRACT", "SUBTRACT", 100, 25, 75.0),
            ("MULTIPLY", "MULTIPLY", 7, 8, 56.0),
            ("DIVIDE exact", "DIVIDE", 144, 12, 12.0),
            ("ADD negative", "ADD", -10, -5, -15.0),
        ]

        for name, op, a, b, expected in tests:
            result = self.call_tool("calculate", {"a": a, "operation": op, "b": b})
            if "error" in result:
                self.add_result("calculate", op, name, False, expected, None, result["error"])
            else:
                passed = abs(result - expected) < 0.001
                self.add_result("calculate", op, name, passed, expected, result)

    def test_echo(self):
        """Test echo tool"""
        print(f"\n{'='*50}\nTesting ECHO\n{'='*50}")

        tests = [
            ("Simple", "Hello, World!"),
            ("Unicode", "Hello 世界"),
            ("Emojis", " 😀 🎉"),
            ("Special", "!@#$%"),
        ]

        for name, msg in tests:
            result = self.call_tool("echo", {"message": msg})
            if "error" in result:
                self.add_result("echo", "echo", name, False, msg, None, result["error"])
            else:
                passed = msg in str(result)
                self.add_result("echo", "echo", name, passed, msg, result)

    def run_all(self):
        """Run all tests"""
        print(f"\n{'='*50}")
        print(f" MCP Test Suite - Simple Client")
        print(f"{'='*50}")

        self.test_calculate()
        self.test_echo()

        self.print_summary()
        self.save_results()


if __name__ == "__main__":
    client = SimpleMCPClient()
    client.run_all()
