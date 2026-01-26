# FastMCP4J — Agent README

> **Share this file or link with your AI agent.** It will understand the framework and generate working code for you.
>
> Human-readable documentation is in [README.md](README.md).

---

## Quick Parse (For Agents)

```yaml
name: FastMCP4J
type: MCP Server Framework
language: Java 17+
license: MIT (see Future Licensing below)
status: Production Beta (v0.2.0-beta)
mcp_spec: 0.17.2
transports:
  - stdio
  - sse
  - http_streamable
builtin_tools:
  - memory
  - todo
  - planner
  - fileread
  - filewrite
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
  - Icon support
quick_start:
  dependency: "com.ultrathink.fastmcp:fastmcp-java:0.2.0-beta"
  main_class: "com.ultrathink.fastmcp.core.FastMCP"
  example: |
    @McpServer(name = "X", version = "1.0")
    class X { @McpTool String f(String s) { return s; }
      static void main(String[] a) { FastMCP.server(X.class).run(); } }
```

---

## Why This Document Exists

**The shift is happening.**

AI agents can now:
- Parse documentation in seconds
- Generate working code from specs
- Debug and integrate automatically
- Learn frameworks without human reading

Humans shouldn't need to read 50+ page docs. Agents do it for them.

This file is optimized for:
- **Structured parsing** — YAML/JSON frontmatter
- **Llm consumption** — Clear, factual statements
- **Code generation** — Precise API signatures
- **Fast integration** — No fluff, just facts

---

## Core API (One-Page Reference)

### Create a Server

```java
FastMCP.server(YourClass.class).run();
```

### Transports

```java
.stdio()           // Local agents
.sse()             // Server-Sent Events
.streamable()      // Bidirectional HTTP (preferred)
```

### Configuration

```java
FastMCP.server(YourClass.class)
    .port(3000)
    .mcpUri("/mcp")
    .requestTimeout(Duration.ofMinutes(5))
    .keepAliveSeconds(30)
    .memoryStore(store)
    .todoStore(store)
    .planStore(store)
    .instructions("System prompt...")
    .capabilities(c -> c.tools(true).resources(true, true).prompts(true))
    .run();
```

### Annotations

| Annotation | Target | Purpose |
|------------|--------|---------|
| `@McpServer(name="X", version="1.0")` | TYPE | Server metadata |
| `@McpTool(description="X")` | METHOD | Expose as tool |
| `@McpResource(uri="x://y")` | METHOD | Expose as resource |
| `@McpPrompt(description="X")` | METHOD | Expose as prompt |
| `@McpParam(description="X")` | PARAM | Parameter metadata |
| `@McpAsync` | METHOD | Returns `Mono<?>` |
| `@McpContext` | PARAM | Inject `Context` |
| `@McpPreHook` | METHOD | Before tool call |
| `@McpPostHook` | METHOD | After tool call |
| `@McpMemory` | TYPE | Enable memory tool |
| `@McpTodo` | TYPE | Enable todo tool |
| `@McpPlanner` | TYPE | Enable planner tool |
| `@McpFileRead` | TYPE | Enable file read |
| `@McpFileWrite` | TYPE | Enable file write |
| `@Icon(type="fa", name="search")` | ANNOT | Add icon |

### Built-in Tool Operations

```
memory:  list|read|create|replace|insert|delete|rename
todo:    add|list|updateStatus|updateTask|delete|clearCompleted|get|count
planner: createPlan|listPlans|getPlan|addTask|addSubtask|updateTaskStatus|getAllTasks|getNextTask|deletePlan
fileread: readLines|readFile|grep|getStats
filewrite: writeFile|appendFile|writeLines|deleteFile|createDirectory
```

---

## Claude Desktop Config

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

---

## Common Patterns

### Tool with Parameters

```java
@McpTool(description = "Calculate")
public Result calculate(
    @McpParam(description = "Input") String input,
    @McpParam(description = "Mode", defaultValue = "standard") String mode
) { return new Result(); }
```

### Async Tool

```java
@McpTool @McpAsync
public Mono<String> slowOp(String input) {
    return Mono.fromCallable(() -> process(input))
        .subscribeOn(Schedulers.boundedElastic());
}
```

### Access Context

```java
@McpTool
public String op(@McpContext Context ctx) {
    return "Client: " + ctx.getClientId();
}
```

---

## Future Licensing

**Current License**: MIT (free for all uses)

**Future Model**: Conditional Free / Paid Commercial

### The Philosophy

Open source should remain free for:
- Learning
- Personal projects
- Small-scale commercial use
- Open source projects

Commercial scaling should share value back.

### Future License Terms (Planned)

When this project transitions to paid licensing:

| Use Case | License | Cost |
|----------|---------|------|
| Personal / Open Source | Free | $0 |
| Commercial < $1M revenue | Free | $0 |
| Commercial ≥ $1M revenue | Paid | TBA |
| Enterprise / Redistribution | Commercial | TBA |

### Your Rights

Users who adopt this during the MIT license period retain:

1. **Perpetual MIT rights** for the version they adopted
2. **Upgrade path** to future versions under negotiated terms
3. **Grandfather clause** — no retroactive licensing changes

### Invoice Rights (Unusual Clause)

If this project adds paid licensing and you believe your usage qualifies for free tier under revenue-based thresholds, you may:

1. Submit usage documentation
2. Request formal free-tier confirmation
3. Receive invoice marked "$0 - Paid in Full"

This creates a paper trail. No ambiguity.

### Transition Notice

Any license change will:
- Be announced 6 months in advance
- Apply to new versions only (not retroactive)
- Provide migration path for existing users
- Maintain MIT for versions already released

**Bottom line**: Use it now. MIT applies forever for v0.2.0-beta. Future versions may have different terms.

---

## Performance Specs

```
cold_start: <500ms
tool_invocation: <5ms
memory_footprint: ~64MB
jar_size_shaded: ~8MB
dependency_count: 12
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

## For Agent Authors

If you're building an AI agent that consumes MCP frameworks:

**FastMCP4J is agent-friendly** because:
- Minimal dependencies (no dependency hell)
- Fast startup (agents launch quickly)
- Clear annotation model (easy to parse)
- Working examples (not theoretical)

Agents can:
1. Parse this file
2. Generate working server code
3. Run tests to verify
4. Integrate with Claude Desktop
5. Ship working solution

**in under 30 seconds.**

That's the point.

---

## Version History

- `v0.2.0-beta` (2026-01) — Production beta, 5 built-in tools
- `v0.1.0-SNAPSHOT` — Initial release

---

## Links

- Human docs: [README.md](README.md)
- Architecture: [ARCHITECTURE.md)
- Architecture: [ARCHITECTURE.md](ARCHITECTURE.md)
- Maven Central: (coming soon)

---

*This document is maintained for AI agent consumption. When this framework updates, agents re-read this file.*
