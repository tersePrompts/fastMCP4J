# FastMCP4J — Agent README

> **Claude Code Skill — FastMCP4J**
>
> Share this with Claude to generate MCP server code.
> Human-readable docs: [README.md](../../../README.md)

---

## Quick Parse

```yaml
name: FastMCP4J
type: MCP Server Framework
language: Java 17+
license: MIT (see Future Licensing below)
status: Production (v0.2.0)
mcp_spec: 0.17.2
transports: [stdio, sse, http_streamable]
builtin_tools: [memory, todo, planner, fileread, filewrite]
annotations:
  - "@McpServer(name, version, icons)"
  - "@McpTool(description, icons)"
  - "@McpResource(uri)"
  - "@McpPrompt(name)"
  - "@McpParam(description, examples, constraints, defaultValue, required)"
  - "@McpAsync"
  - "@McpContext"
  - "@McpPreHook"
  - "@McpPostHook"
  - "@McpMemory"
  - "@McpTodo"
  - "@McpPlanner"
  - "@McpFileRead"
  - "@McpFileWrite"
dependency:
  maven: "io.github.terseprompts.fastmcp:fastmcp-java:0.2.0"
  gradle: "implementation 'io.github.terseprompts.fastmcp:fastmcp-java:0.2.0'"
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
public Mono<String> process(@McpContext Context ctx, String input) {
    return Mono.fromCallable(() -> {
        ctx.reportProgress(50, "Processing...");
        return slowOperation(input);
    });
}
```

### Add icons

```java
@McpServer(
    name = "my-server",
    icons = {
        "data:image/svg+xml;base64,...:image/svg+xml:64x64:light",
        "data:image/svg+xml;base64,...:image/svg+xml:64x64:dark"
    }
)
@McpTool(description = "My tool", icons = {"https://example.com/icon.png"})
public class MyServer { }
```

### Resources & Prompts

```java
@McpResource(uri = "config://settings")
public String getSettings() {
    return "{\"theme\": \"dark\"}";
}

@McpPrompt(name = "code-review")
public String codeReviewPrompt(@McpParam(description = "Code") String code) {
    return "Review: " + code;
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
public class MyServer {
    public static void main(String[] args) {
        FastMCP.server(MyServer.class).stdio().run();
    }
}
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

---

## Built-in Tools

```
@McpMemory     → list|read|create|replace|insert|delete|rename
@McpTodo       → add|list|updateStatus|updateTask|delete|clearCompleted
@McpPlanner    → createPlan|listPlans|getPlan|addTask|addSubtask
@McpFileRead   → readLines|readFile|grep|getStats
@McpFileWrite  → writeFile|appendFile|writeLines|deleteFile|createDirectory
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
| Built-in tools | ❌ | ✅ 5 sets |

---

## Documentation Links

- [README.md](../../../README.md) — Main documentation
- [ARCHITECTURE.md](../../../ARCHITECTURE.md) — How it works
- [ROADMAP.md](../../../ROADMAP.md) — What's coming
- [CONTRIBUTING.md](../../../CONTRIBUTING.md) — Contributing
- [CHANGELOG.md](../../../CHANGELOG.md) — Version history

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
