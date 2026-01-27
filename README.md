<div align="center">

# FastMCP4J

### Java library for building MCP servers — annotation-driven, minimal dependencies

**[AI Agents →](.claude/skill/fastmcp4j/skill.md)** Share this skill with Claude for code generation

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-168%20Passing-brightgreen.svg)](src/test/java)

**Lightweight. 12 dependencies. No containers.**

Just annotate and run. See below →

</div>

**Note**: Alpha release (v0.2.0) — comprehensive tests, API may evolve.

---

## Quick Start (2 minutes)

### Add dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.terseprompts.fastmcp</groupId>
    <artifactId>fastmcp-java</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Gradle:**
```groovy
dependencies {
    implementation 'io.github.terseprompts.fastmcp:fastmcp-java:0.2.0'
}
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

## Who This Is For

| You want to... | FastMCP4J |
|----------------|-----------|
| Expose Java tools to AI agents | ✅ Perfect fit |
| Build MCP servers quickly | ✅ Annotation-driven, minimal code |
| Add MCP to existing Spring app | ✅ Drop-in, no framework lock-in |
| Lightweight MCP-only solution | ✅ 12 dependencies, not 50+ |
| Fast startup & low memory | ✅ <500ms cold start, ~64MB |

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
public Mono<String> process(@McpContext Context ctx, String input) {
    return Mono.fromCallable(() -> {
        ctx.reportProgress(50, "Processing...");
        return slowOperation(input);
    });
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
    // All tools enabled, zero implementation needed
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

---

## Icons

Add visual polish to your server, tools, resources, and prompts.

```java
@McpServer(
    name = "my-server",
    icons = {
        "data:image/svg+xml;base64,...:image/svg+xml:64x64:light",
        "data:image/svg+xml;base64,...:image/svg+xml:64x64:dark"
    }
)
@McpTool(
    description = "My tool",
    icons = {"https://example.com/icon.png"}
)
public class MyServer { }
```

---

## Resources & Prompts

```java
@McpResource(uri = "config://settings")
public String getSettings() {
    return "{\"theme\": \"dark\"}";
}

@McpPrompt(name = "code-review")
public String codeReviewPrompt(@McpParam(description = "Code to review") String code) {
    return "Review this code:\n" + code;
}
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
| `@McpParam` | PARAMETER | Add description, examples, constraints, defaults |
| `@McpAsync` | METHOD | Make tool async (return `Mono<?>`) |
| `@McpContext` | PARAMETER | Inject request context |
| `@McpPreHook` | METHOD | Run before tool call |
| `@McpPostHook` | METHOD | Run after tool call |

**@McpParam advanced options:**
```java
@McpTool(description = "Create task")
public String createTask(
    @McpParam(
        description = "Task name",
        examples = {"backup", "sync"},
        constraints = "Cannot be empty",
        defaultValue = "default",
        required = false
    ) String taskName
) { return "Created: " + taskName; }
```

---

## Hooks — Run Code Before/After Tools

Two hook types supported:

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

**Context** (same type used by @McpContext, @McpPreHook, @McpPostHook):
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

        // Access request headers (e.g., for auth)
        Map<String, String> headers = context.getRequestHeaders();
        String authHeader = headers.get("Authorization");

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
- `getRequestHeaders()` — Client request headers (e.g., auth tokens, custom headers)
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

### Lightweight

| Framework | Dependencies | Best For |
|-----------|--------------|----------|
| Spring AI | 50+ jars | Full-stack AI apps |
| LangChain4j | 30+ jars | Enterprise AI pipelines |
| Quarkus AI | 40+ jars | Cloud-native microservices |
| FastMCP4J | **12 jars** | **MCP servers only** |

### Fast & focused

- Cold start: <500ms
- Tool invocation: <5ms
- Memory: ~64MB
- Purpose-built for MCP — not a general AI framework

---

## Documentation

- [Architecture](ARCHITECTURE.md) — How it works
- [Roadmap](ROADMAP.md) — What's next
- [Contributing](CONTRIBUTING.md) — PRs welcome
- [Changelog](CHANGELOG.md) — Version history
- [Claude Skill](.claude/skill/fastmcp4j/skill.md) — For AI agents

---

## License

MIT © 2026

---

<div align="center">

**Less boilerplate. More shipping.**

[Get started](#quick-start-2-minutes) • [Examples](https://github.com/tersePrompts/fastMCP4J/blob/main/src/test/java/com/ultrathink/fastmcp/example/EchoServer.java) • [Docs](#documentation)

Made with ❤️ for the Java community
</div>
