# FastMCP4J Integration Tests

Python MCP client tests for validating FastMCP4J server functionality across different transports.

## Overview

| Test File | Transport | Server | Port | Purpose |
|-----------|-----------|--------|------|---------|
| `test_stdio_quick.py` | STDIO | StdioFullServer | N/A | Tests standard input/output transport |
| `test_sse_quick.py` | HTTP SSE | SimpleSseServer | 3001 | Tests Server-Sent Events transport |
| `test_streamable_quick.py` | HTTP Streamable | SimpleStreamableServer | 3002 | Tests bidirectional HTTP streaming |

## Running Tests Locally

### Prerequisites
```bash
# Install Python MCP SDK
pip install mcp

# Build the project (if needed)
mvn clean package -DskipTests
```

### STDIO Test (server spawned via Maven)
```bash
python python-tests/test_stdio_quick.py
```

### SSE Test (server must be running on port 3001)
```bash
# Terminal 1: Start server
mvn exec:java -Dexec.mainClass=io.github.terseprompts.fastmcp.example.SimpleSseServer -Dexec.classpathScope=test

# Terminal 2: Run test
python python-tests/test_sse_quick.py
```

### Streamable Test (server must be running on port 3002)
```bash
# Terminal 1: Start server
mvn exec:java -Dexec.mainClass=io.github.terseprompts.fastmcp.example.SimpleStreamableServer -Dexec.classpathScope=test

# Terminal 2: Run test
python python-tests/test_streamable_quick.py
```

## CI/CD Usage

These tests are integrated into GitHub Actions workflows:
- `.github/workflows/test-stdio.yml`
- `.github/workflows/test-sse.yml`
- `.github/workflows/test-streamable.yml`

## Test Output Format

Tests use structured logging with clear sections:
```
============================================================
  MCP SSE Transport Test
============================================================
Target: http://127.0.0.1:3001/sse
Max retries: 5

>>> Connection attempt 1/5...
  ✅ Connection established: Connected to http://127.0.0.1:3001/sse
  📋 Session initialized

>>> Listing available tools...
  Found 2 tools:
    - add
    - echo

>>> Testing 'add' tool...
  ✅ add(10, 5): 15.0

>>> Testing 'echo' tool...
  ✅ echo('Hello CI/CD'): Echo: Hello CI/CD

============================================================
  ✅ ALL TESTS PASSED
============================================================
```

## Transport Comparison

| Transport | Use Case | Pros | Cons |
|-----------|-----------|------|------|
| **STDIO** | CLI tools, local agents | Simple, no ports | Single connection |
| **SSE** | Web clients, long-lived | Real-time updates | One-way streaming |
| **Streamable** | Web apps, bidirectional | Full-duplex, resumable | More complex |
