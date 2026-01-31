# FastMCP4J — Code Generation Skill

You are working with **FastMCP4J**, a Java library for building MCP (Model Context Protocol) servers.

## Package

**Current package:** `io.github.terseprompts.fastmcp`

**Maven coordinates:**
```xml
<dependency>
    <groupId>io.github.terseprompts.fastmcp</groupId>
    <artifactId>fastmcp-java</artifactId>
    <version>0.3.1-beta</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'io.github.terseprompts.fastmcp:fastmcp-java:0.3.1-beta'
```

## Core Imports

```java
import io.github.terseprompts.fastmcp.*;
import io.github.terseprompts.fastmcp.annotation.*;
import io.github.terseprompts.fastmcp.annotation.McpParam;
import io.github.terseprompts.fastmcp.context.Context;
import reactor.core.publisher.Mono;
```

## Essential Annotations

| Annotation | Target | Purpose |
|------------|--------|---------|
| `@McpServer` | TYPE | Define MCP server |
| `@McpTool` | METHOD | Expose as callable tool |
| `@McpResource` | METHOD | Expose as resource |
| `@McpPrompt` | METHOD | Expose as prompt template |
| `@McpParam` | PARAMETER | Add description, examples, constraints |
| `@McpAsync` | METHOD | Make tool async (return `Mono<?>`) |
| `@McpContext` | PARAMETER | Inject request context |
| `@McpPreHook` | METHOD | Run before tool call |
| `@McpPostHook` | METHOD | Run after tool call |
| `@McpMemory` | TYPE | Enable memory tools |
| `@McpTodo` | TYPE | Enable todo/task management |
| `@McpPlanner` | TYPE | Enable planning tools |
| `@McpFileRead` | TYPE | Enable file reading tools |
| `@McpFileWrite` | TYPE | Enable file writing tools |
| `@McpBash` | TYPE | Enable shell command execution |
| `@McpTelemetry` | TYPE | Enable metrics and tracing |

## Code Patterns

### Basic Server

```java
@McpServer(name = "MyServer", version = "1.0")
public class MyServer {

    @McpTool(description = "Add two numbers")
    public int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        FastMCP.server(MyServer.class)
            .stdio()
            .run();
    }
}
```

### Async Tool

```java
@McpTool(description = "Process data asynchronously")
@McpAsync
public Mono<String> process(String input) {
    return Mono.fromCallable(() -> slowOperation(input));
}
```

### Tool with Context

```java
@McpTool(description = "Get client info")
public String getClientInfo(@McpContext Context ctx) {
    return "Client: " + ctx.getClientId();
}
```

### Tool with Parameter Metadata

```java
@McpTool(description = "Create task")
public String createTask(
    @McpParam(description = "Task name", required = true) String taskName,
    @McpParam(description = "Priority", defaultValue = "medium") String priority
) {
    return "Created: " + taskName;
}
```

### Multi-Class Modules

```java
@McpServer(
    name = "MyServer",
    version = "1.0",
    modules = {StringTools.class, MathTools.class}
)
public class MyServer { }
```

### Package Scanning

```java
@McpServer(
    name = "MyServer",
    version = "1.0",
    scanBasePackage = "com.example.tools"
)
public class MyServer { }
```

### Built-in Tools

```java
@McpServer(name = "MyServer", version = "1.0")
@McpMemory      // AI remembers
@McpTodo        // AI manages tasks
@McpPlanner     // AI breaks tasks into steps
@McpFileRead    // AI reads files
@McpFileWrite   // AI writes files
@McpBash        // AI executes shell commands
@McpTelemetry   // Metrics and tracing
public class MyServer { }
```

## Transports

```java
FastMCP.server(MyServer.class)
    .stdio()       // CLI tools, local agents
    .sse()         // Web clients, long-lived connections
    .streamable()  // Bidirectional streaming
    .run();
```

## Configuration

```java
FastMCP.server(MyServer.class)
    .port(3000)
    .requestTimeout(Duration.ofMinutes(5))
    .keepAliveSeconds(30)
    .capabilities(c -> c
        .tools(true)
        .resources(true, true)
        .prompts(true))
    .run();
```

## Hooks

```java
@McpPreHook(toolName = "*", order = 1)
void authenticate(Map<String, Object> args) {
    // Validate before all tools
}

@McpPostHook(toolName = "calculate", order = 1)
void logResult(Map<String, Object> args, Object result) {
    // Log after specific tool
}
```

## Main Class

```java
io.github.terseprompts.fastmcp.core.FastMCP
```

## Resource/Prompt Patterns

```java
@McpResource(uri = "config://settings")
public String getSettings() {
    return "{\"theme\": \"dark\"}";
}

@McpPrompt(name = "code-review")
public String codeReviewPrompt(@McpParam(description = "Code") String code) {
    return "Review this code:\n" + code;
}
```

## When Generating Code

1. Always use package `io.github.terseprompts.fastmcp`
2. Prefer `@McpTool` for any method that should be exposed to AI
3. Add `description` to tools and parameters
4. Use `@McpAsync` + `Mono<?>` for I/O operations
5. Use `@McpContext` to inject Context when needed (logging, progress, client info)
6. Default to `.stdio()` for CLI tools, `.streamable()` for web
7. Add built-in tool annotations (`@McpMemory`, etc.) when user needs those features
