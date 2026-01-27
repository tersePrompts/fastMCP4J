# CI/CD Testing for MCP Servers

> **Skill for testing MCP servers in CI/CD environments**
>
> This knowledge comes from debugging FastMCP4J integration tests in GitHub Actions.

## Key Lessons

### 1. Server Startup - Don't Poll with HTTP During Initialization

**❌ Problem Pattern:**
```bash
# This causes MCP server to hang in CI!
for i in {1..90}; do
    curl -s -f http://localhost:3001/mcp  # ← Interferes with startup!
    sleep 1
done
```

**✅ Solution: Fixed Wait + Health Checks**
```bash
# Start server in background
mvn exec:java -Dexec.mainClass=... > server.log 2>&1 &
SERVER_PID=$!

# Fixed wait - no HTTP requests during startup
sleep 15

# Check process is alive
kill -0 $SERVER_PID || exit 1

# Check port is listening
ss -ln | grep 3001 || exit 1

# Now ready for tests
```

**Why?** MCP servers need to fully initialize before receiving client connections. HTTP requests during initialization can cause race conditions or connection issues in containerized CI environments.

### 2. Use 127.0.0.1 Instead of localhost

**In CI environments**, DNS resolution for `localhost` can be slow or fail. Use `127.0.0.1` directly.

```python
url = "http://127.0.0.1:3001/sse"  # ✅ More reliable in CI
url = "http://localhost:3001/sse"   # ❌ Can have DNS issues
```

### 3. Test Server Should Be Lightweight

Full-featured servers with many built-in tools (@McpMemory, @McpTodo, etc.) take too long to start.

**✅ Create Simple Test Servers:**
```java
@McpServer(name = "simple-sse", version = "1.0")
public class SimpleSseServer {
    @McpTool(description = "Add two numbers")
    public double add(double a, double b) { return a + b; }

    @McpTool(description = "Echo message")
    public String echo(String message) { return "Echo: " + message; }
}
```

### 4. Python Test Logging Structure

Make test output readable in GitHub Actions with clear sections:

```python
def log_section(title: str) -> None:
    print(f"\n{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")

def log_step(step: str) -> None:
    print(f"\n>>> {step}")

def log_result(test: str, result: str, success: bool = True) -> None:
    status = "✅" if success else "❌"
    print(f"  {status} {test}: {result}")
```

**Output:**
```
============================================================
  MCP SSE Transport Test
============================================================

>>> Connection attempt 1/5...
  ✅ Connection established: Connected to http://127.0.0.1:3001/sse
  📋 Session initialized

>>> Testing 'add' tool...
  ✅ add(10, 5): 15.0

============================================================
  ✅ ALL TESTS PASSED
============================================================
```

### 5. GitHub Actions Workflow Structure

Use emojis in step names for better visibility:

```yaml
steps:
  - name: 📦 Checkout code
  - name: ☕ Set up JDK 17
  - name: 🐍 Set up Python
  - name: 💾 Cache Maven packages
  - name: 📦 Install Python MCP SDK
  - name: 🔨 Build project
  - name: 🚀 Start SSE server
  - name: 🧪 Run SSE MCP test
  - name: 📋 Server logs
  - name: 🧹 Cleanup
```

### 6. Use `::group::` for Collapsible Test Output

```yaml
- name: 🧪 Run SSE MCP test
  run: |
    echo "::group::Test Execution"
    python python-tests/test_sse_quick.py
    echo "::endgroup::"
```

This creates a collapsible section in GitHub Actions UI, keeping the main log view clean while allowing detailed inspection when needed.

## Transport Comparison

| Transport | Best For | Server Binding | Notes |
|-----------|----------|----------------|-------|
| **STDIO** | CLI tools, local agents | N/A | Spawns server via mvn exec:java |
| **SSE** | Web clients, long-lived | :3001/sse | Server-Sent Events, one-way streaming |
| **Streamable** | Web apps, bidirectional | :3002/mcp | HTTP POST + SSE, resumable |

## Common Pitfalls

| Issue | Symptom | Solution |
|-------|----------|----------|
| Polling with curl during startup | Tests hang | Use fixed wait + port check |
| Using localhost | DNS delays in CI | Use 127.0.0.1 |
| Heavy test servers | Timeout before test | Create minimal servers |
| Missing PYTHONUNBUFFERED | Delayed output | Set env: PYTHONUNBUFFERED=1 |
| No server logs on failure | Can't debug | Always show logs on failure |
