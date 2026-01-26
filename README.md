<div align="center">

# FastMCP4J

### Java library for building MCP servers — annotation-driven, zero bloat

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-168%20Passing-brightgreen.svg)](src/test/java)

**Zero bloat. 12 dependencies. No containers.**

Just annotate and run. See below →

</div>

---

## Quick Start (2 minutes)

### Add dependency

**Maven:**
```xml
<dependency>
    <groupId>com.ultrathink.fastmcp</groupId>
    <artifactId>fastmcp-java</artifactId>
    <version>0.2.0-beta</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'com.ultrathink.fastmcp:fastmcp-java:0.2.0-beta'
```

### Create your server

```java
@McpServer(name = "Assistant", version = "1.0")
public class MyAssistant {

    @McpTool(description = "Summarize text")
    public String summarize(@McpParam(description = "Text") String text) {
        return "Summary: " + text.substring(0, Math.min(100, text.length()));
    }

    public static void main(String[] args) {
        FastMCP.server(MyAssistant.class)
            .stdio()           // or .sse() or .streamable()
            .run();
    }
}
```

### Run it

```bash
mvn exec:java -Dexec.mainClass="com.example.MyAssistant"
```

**That's it. Your MCP server is running.**

**Working example**: [EchoServer.java](https://github.com/tersePrompts/fastMCP4J/blob/main/src/test/java/com/ultrathink/fastmcp/example/EchoServer.java)

---

## How to Use

### Make a tool

```java
@McpTool(description = "Add two numbers")
public int add(int a, int b) {
    return a + b;
}
```

### Make it async

```java
@McpTool(description = "Process data")
@McpAsync  // ← just add this
public Mono<String> process(String input) {
    return Mono.fromCallable(() -> slowOperation(input));
}
```

### Add memory

```java
@McpServer(name = "MyServer", version = "1.0")
@McpMemory  // ← just add this
public class MyServer {
    // AI now remembers things across sessions
}
```

### Add all built-in tools

```java
@McpServer(name = "MyServer", version = "1.0")
@McpMemory     // AI remembers
@McpTodo       // AI manages tasks
@McpPlanner    // AI breaks tasks into steps
@McpFileRead   // AI reads your files
@McpFileWrite  // AI writes files
public class MyServer {
    // All tools enabled, zero implementation
}
```

### Choose transport

```java
FastMCP.server(MyServer.class)
    .stdio()       // For CLI tools, local agents
    .sse()         // For web clients, long-lived connections
    .streamable()  // For bidirectional streaming (recommended)
    .run();
```

### Configure port, timeout, capabilities

```java
FastMCP.server(MyServer.class)
    .port(3000)                              // HTTP port
    .requestTimeout(Duration.ofMinutes(5))   // Request timeout
    .keepAliveSeconds(30)                     // Keep-alive interval
    .capabilities(c -> c
        .tools(true)
        .resources(true, true)
        .prompts(true))
    .run();
```

### Use with Claude Desktop

Add to `claude_desktop_config.json`:

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

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

### Use with Claude Code

```bash
Claude mcp add --transport http myserver http://localhost:3000/mcp
```

---

## Built-in Tools

Add ONE annotation, get complete functionality.

| Annotation | Tools You Get |
|------------|---------------|
| `@McpMemory` | list, read, create, replace, insert, delete, rename |
| `@McpTodo` | add, list, updateStatus, updateTask, delete, clearCompleted |
| `@McpPlanner` | createPlan, listPlans, getPlan, addTask, addSubtask |
| `@McpFileRead` | readLines, readFile, grep, getStats |
| `@McpFileWrite` | writeFile, appendFile, writeLines, deleteFile, createDirectory |

---

## Annotations Reference

| Annotation | Target | Purpose |
|------------|--------|---------|
| `@McpServer` | TYPE | Define your MCP server |
| `@McpTool` | METHOD | Expose as callable tool |
| `@McpResource` | METHOD | Expose as resource |
| `@McpPrompt` | METHOD | Expose as prompt template |
| `@McpParam` | PARAMETER | Add description, examples, constraints |
| `@McpAsync` | METHOD | Make tool async (return `Mono<?>`) |
| `@McpContext` | PARAMETER | Inject request context |
| `@McpPreHook` | METHOD | Run before tool call |
| `@McpPostHook` | METHOD | Run after tool call |

---

## Hooks — Run Code Before/After Tools

**@McpPreHook** — Runs before any tool is called.

**@McpPostHook** — Runs after any tool completes.

Use for logging, validation, metrics, audit trails.

```java
@McpServer(name = "MyServer", version = "1.0")
public class MyServer {

    @McpTool(description = "Calculate")
    public int calculate(int x, int y) {
        return x + y;
    }

    @McpPreHook
    void logBefore(ToolContext ctx) {
        System.out.println("Starting: " + ctx.getToolName());
    }

    @McpPostHook
    void logAfter(ToolContext ctx) {
        System.out.println("Finished: " + ctx.getToolName());
    }

    public static void main(String[] args) {
        FastMCP.server(MyServer.class).stdio().run();
    }
}
```

**Available in ToolContext:**
- `getToolName()` — Name of the tool being called
- `getArguments()` — Arguments passed to the tool
- `getStartTime()` — When the tool started

---

## Context Access — Request Metadata

**@McpContext** — Inject request context into your tool.

Access client info, session data, request metadata.

```java
@McpServer(name = "MyServer", version = "1.0")
public class MyServer {

    @McpTool(description = "Get client info")
    public String getClientInfo(@McpContext Context context) {
        return "Client: " + context.getClientId();
    }

    @McpTool(description = "Get session ID")
    public String getSessionId(@McpContext Context context) {
        return "Session: " + context.getSessionId();
    }

    @McpTool(description = "Read file with context")
    public String readFile(@McpContext Context context, String path) {
        context.info("Reading file: " + path);
        // ... read file
        return "Content";
    }

    public static void main(String[] args) {
        FastMCP.server(MyServer.class).stdio().run();
    }
}
```

**Context capabilities:**
- `getClientId()` — Client identifier
- `getSessionId()` — Session identifier
- `getToolName()` — Current tool name
- `info(String)` — Log info message
- `warning(String)` — Log warning
- `error(String)` — Log error
- `reportProgress(int, String)` — Report progress percentage
- `listResources()` — List available resources
- `listPrompts()` — List available prompts

---

## Why FastMCP4J?

### Less code

**Raw MCP SDK**: 35+ lines per tool
**FastMCP4J**: ~8 lines per tool

### No bloat

| Framework | Dependencies |
|-----------|--------------|
| Spring AI | 50+ jars |
| LangChain4j | 30+ jars |
| FastMCP4J | **12 jars** |

### Fast

- Cold start: <500ms
- Tool invocation: <5ms
- Memory: ~64MB

---

## Documentation

- [Architecture](ARCHITECTURE.md) — How it works
- [Roadmap](ROADMAP.md) — What's next
- [Contributing](CONTRIBUTING.md) — PRs welcome
- [Changelog](CHANGELOG.md) — Version history
- [Agent README](agent-readme.md) — For AI agents

---

## License

MIT © 2026

**Future licensing note**: Versions released under MIT remain MIT forever. Future versions may introduce paid licensing for enterprise use. See [agent-readme.md](agent-readme.md) for details.

---

<div align="center">

**Less boilerplate. More shipping.**

[Get started](#quick-start-2-minutes) • [Examples](https://github.com/tersePrompts/fastMCP4J/blob/main/src/test/java/com/ultrathink/fastmcp/example/EchoServer.java) • [Docs](#documentation)

Made with ❤️ for the Java community
</div>
