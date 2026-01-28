# FastMCP4J — Agent README

> **Share this file or link with your AI agent.** It will understand the framework and generate working code for you.
>
> Human-readable documentation is in [README.md](README.md).

---

## Quick Parse

```yaml
name: FastMCP4J
type: MCP Server Framework
language: Java 17+
license: MIT (see Future Licensing below)
status: Production Beta (v0.3.0-beta)
mcp_spec: 0.17.2
json_schema: 2020-12
transports: [stdio, sse, http_streamable]
builtin_tools: [memory, todo, planner, fileread, filewrite, bash]
annotations:
  - "@McpServer"
  - "@McpTool"
  - "@McpResource"
  - "@McpPrompt"
  - "@McpParam"
  - "@McpAsync"
  - "@McpContext"
  - "@McpPreHook"
  - "@McpPostHook"
  - "@McpMemory"
  - "@McpTodo"
  - "@McpPlanner"
  - "@McpFileRead"
  - "@McpFileWrite"
  - "@McpBash"
dependency:
  maven: "com.ultrathink.fastmcp:fastmcp-java:0.3.0-beta"
  gradle: "implementation 'com.ultrathink.fastmcp:fastmcp-java:0.3.0-beta'"
main_class: "com.ultrathink.fastmcp.core.FastMCP"
example: |
  @McpServer(name="X", version="1.0") class X {
    @McpTool String f(String s) { return s; }
    static void main(String[] a) { FastMCP.server(X.class).stdio().run(); }
  }
```

---

## How to Use

### Make a tool

```java
@McpTool(description = "Add two numbers")
public int add(int a, int b) { return a + b; }
```

### Make it async

```java
@McpTool @McpAsync
public Mono<String> process(String input) {
    return Mono.fromCallable(() -> slowOperation(input));
}
```

### Add built-in tools

```java
@McpServer(name = "MyServer", version = "1.0")
@McpMemory     // AI remembers
@McpTodo       // AI manages tasks
@McpPlanner    // AI plans
@McpFileRead   // AI reads files
@McpFileWrite  // AI writes files
@McpBash       // AI executes shell commands
public class MyServer {
    public static void main(String[] args) {
        FastMCP.server(MyServer.class).stdio().run();
    }
}
```

### Bash tool with restrictions

```java
@McpBash(
    timeout = 60,
    visibleAfterBasePath = "/home/user/projects/*",
    notAllowedPaths = {"/etc", "/sys", "/proc"}
)
```

### Choose transport

```java
FastMCP.server(MyServer.class)
    .stdio()       // CLI tools, local agents
    .sse()         // Web clients, long-lived connections
    .streamable()  // Bidirectional streaming (recommended)
    .run();
```

### Configure

```java
FastMCP.server(MyServer.class)
    .port(3000)
    .requestTimeout(Duration.ofMinutes(5))
    .keepAliveSeconds(30)
    .capabilities(c -> c.tools(true).resources(true, true).prompts(true))
    .run();
```

### Claude Desktop config

```json
{
  "mcpServers": {
    "my-server": {
      "transport": {
        "type": "http_streamable",
        "url": "http://localhost:3000/mcp"
      }
    }
  }
}
```

### Claude Code command

```bash
Claude mcp add --transport http myserver http://localhost:3000/mcp
```

---

## Annotations Reference

| Annotation | Target | Purpose |
|------------|--------|---------|
| `@McpServer` | TYPE | Define server |
| `@McpTool` | METHOD | Expose as tool |
| `@McpResource` | METHOD | Expose as resource |
| `@McpPrompt` | METHOD | Expose as prompt |
| `@McpParam` | PARAMETER | Add metadata |
| `@McpAsync` | METHOD | Return `Mono<?>` |
| `@McpContext` | PARAMETER | Inject context |
| `@McpPreHook` | METHOD | Before tool |
| `@McpPostHook` | METHOD | After tool |
| `@McpMemory` | TYPE | Enable memory tool |
| `@McpTodo` | TYPE | Enable todo tool |
| `@McpPlanner` | TYPE | Enable planner tool |
| `@McpFileRead` | TYPE | Enable file read |
| `@McpFileWrite` | TYPE | Enable file write |
| `@McpBash` | TYPE | Enable bash with path restrictions |

---

## Built-in Tools

```
@McpMemory     → list|read|create|replace|insert|delete|rename
@McpTodo       → add|list|updateStatus|updateTask|delete|clearCompleted
@McpPlanner    → createPlan|listPlans|getPlan|addTask|addSubtask
@McpFileRead   → readLines|readFile|grep|getStats
@McpFileWrite  → writeFile|appendFile|writeLines|deleteFile|createDirectory
@McpBash       → bash (OS-aware: cmd.exe|zsh|bash)
```

---

## Performance

```
cold_start: <500ms
tool_invocation: <5ms
memory_footprint: ~64MB
jar_size_shaded: ~8MB
dependencies: 12
```

---

## Test Status

```
total_tests: 168
passing: 168
failing: 0
coverage: 95%
```

---

## Why This Library

| Metric | Raw SDK | FastMCP4J |
|--------|---------|-----------|
| Lines per tool | 35+ | ~8 |
| Dependencies | 1 | 12 |
| Built-in tools | ❌ | ✅ 6 sets |

---

## Documentation Links

- [README.md](README.md) — Main documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) — How it works
- [ROADMAP.md](ROADMAP.md) — What's coming
- [CONTRIBUTING.md](CONTRIBUTING.md) — Contributing
- [CHANGELOG.md](CHANGELOG.md) — Version history

---

## Future Licensing

**Current**: MIT (free for all uses)

**Future**: May add paid licensing for enterprise use

**Your rights**:
- MIT versions remain MIT forever
- Grandfather clause for existing users
- See "Invoice Rights" in full documentation

---

## For Agents Generating Code

When generating MCP server code:

1. Use `@McpServer` on the class
2. Use `@McpTool` on methods to expose
3. Use `@McpParam` for parameter descriptions
4. Call `FastMCP.server(YourClass.class).run()` in main
5. Choose transport: `.stdio()`, `.sse()`, or `.streamable()`

**Example server**: [EchoServer.java](https://github.com/tersePrompts/fastMCP4J/blob/main/src/test/java/com/ultrathink/fastmcp/example/EchoServer.java)

---

*This document is maintained for AI agent consumption. When the framework updates, agents re-read this file.*
