<div align="center">

# FastMCP4J

### A batteries-included framework for building MCP servers in Java

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-168%20Passing-brightgreen.svg)](src/test/java)

Bringing the simplicity of [FastMCP](https://gofastmcp.com) to Java — reduce boilerplate from 30+ lines to ~10 lines per MCP server.

</div>

---

## Table of Contents

- [Quick Start](#quick-start)
- [Features](#features)
- [Installation](#installation)
- [Core Concepts](#core-concepts)
- [Examples](#examples)
- [Configuration](#configuration)
- [Available Tools](#available-tools)
- [Testing](#testing)
- [Contributing](#contributing)

---

## Quick Start

Create an MCP server in just 10 lines:

```java
@McpServer(name = "Calculator", version = "1.0.0")
public class CalculatorServer {

    @McpTool(description = "Add two numbers")
    public int add(
        @McpParam(description = "First number") int a,
        @McpParam(description = "Second number") int b
    ) {
        return a + b;
    }

    public static void main(String[] args) {
        FastMCP.server(CalculatorServer.class)
            .streamable()
            .port(3000)
            .run();
    }
}
```

---

## Features

### Core MCP Elements

| Feature | Annotation | Description |
|---------|------------|-------------|
| **Tools** | `@McpTool` | Expose methods as callable MCP tools |
| **Resources** | `@McpResource` | Serve data/content with URI access |
| **Prompts** | `@McpPrompt` | Define LLM interaction templates |
| **Parameters** | `@McpParam` | Rich parameter metadata (descriptions, examples, constraints) |

### Built-in Tools

| Annotation | Tool | Description |
|------------|------|-------------|
| `@McpMemory` | memory | Persistent storage for cross-session learning |
| `@McpTodo` | todo | Task management with priorities and status |
| `@McpPlanner` | planner | Task decomposition and planning |
| `@McpFileRead` | fileread | File reading, grep, and statistics |
| `@McpFileWrite` | filewrite | File writing and directory operations |

### Transport Options

| Method | Transport | Use Case |
|--------|-----------|----------|
| `.stdio()` | STDIO | CLI tools, local agents (default) |
| `.sse()` | HTTP SSE | Long-lived connections, server push |
| `.streamable()` | HTTP Streamable | Bidirectional streaming, latest protocol |

### Advanced Features

<details>
<summary><b>Async Support</b> - Reactive operations with Project Reactor</summary>

```java
@McpTool(description = "Process data asynchronously")
@McpAsync
public Mono<String> processData(String datasetId) {
    return Mono.fromCallable(() -> {
        // Long-running operation
        return "Processed: " + datasetId;
    }).delayElement(Duration.ofSeconds(5));
}
```
</details>

<details>
<summary><b>Hooks</b> - Pre/post execution hooks</summary>

```java
@McpPreHook
public void beforeToolCall(ToolContext ctx) {
    System.out.println("Calling: " + ctx.getToolName());
}

@McpPostHook
public void afterToolCall(ToolContext ctx) {
    System.out.println("Completed: " + ctx.getToolName());
}
```
</details>

<details>
<summary><b>Context Access</b> - Access request metadata</summary>

```java
@McpTool(description = "Get client info")
public String getClientInfo(@McpContext Context context) {
    return "Client ID: " + context.getClientId();
}
```
</details>

<details>
<summary><b>Icons</b> - Visual icons for tools/resources</summary>

```java
@McpTool(
    description = "Search files",
    icons = {
        @Icon(type = "fa", name = "search"),
        @Icon(type = "uri", uri = "file:///search.png")
    }
)
public String search(String query) { ... }
```
</details>

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.ultrathink.fastmcp</groupId>
    <artifactId>fastmcp-java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.ultrathink.fastmcp:fastmcp-java:0.1.0-SNAPSHOT'
```

---

## Core Concepts

### Server Definition

Use `@McpServer` to define your MCP server:

```java
@McpServer(
    name = "MyServer",
    version = "1.0.0",
    description = "My awesome MCP server"
)
public class MyServer { }
```

### Fluent Builder API

Configure your server with a fluent API:

```java
FastMCP.server(MyServer.class)
    // Transport selection
    .streamable()              // or .stdio() or .sse()

    // Network configuration
    .port(3000)                // HTTP port (default: 8080)
    .mcpUri("/api/mcp")        // MCP endpoint (default: /mcp)
    .baseUrl("https://example.com")

    // Timeouts
    .requestTimeout(Duration.ofMinutes(5))
    .keepAliveSeconds(30)

    // Server capabilities
    .capabilities(c -> c
        .tools(true)
        .resources(true, true)
        .prompts(true)
        .logging()
        .completions())

    // Custom stores
    .memoryStore(customMemoryStore)
    .todoStore(customTodoStore)
    .planStore(customPlanStore)

    // Instructions
    .instructions("You are a helpful assistant...")

    .run();
```

### Server Capabilities Builder

```java
FastMCP.server(MyServer.class)
    .capabilities(capabilities -> capabilities
        .tools(true)                    // Enable tools with listChanged
        .resources(true, true)          // Enable resources with subscribe + listChanged
        .prompts(true)                  // Enable prompts with listChanged
        .noTools()                      // Disable tools
        .noResources()                  // Disable resources
        .noPrompts()                    // Disable prompts
        .logging()                      // Enable logging capability
        .completions())                 // Enable completions capability
    .run();
```

---

## Examples

### Enhanced Parameters

```java
@McpTool(name = "search_files", description = "Search files with advanced filtering")
public String searchFiles(
    @McpParam(
        description = "Directory path to search in",
        examples = {"/home/user/docs", "./src/main/java"},
        constraints = "Must be a valid directory path",
        hints = "Use '.' for current directory"
    )
    String directory,

    @McpParam(
        description = "File pattern to match",
        examples = {"*.java", "**/*.txt"},
        constraints = "Must be a valid glob pattern",
        hints = "Use ** for recursive search"
    )
    String pattern,

    @McpParam(
        description = "Search recursively",
        defaultValue = "true",
        required = false
    )
    boolean recursive
) {
    // Implementation
    return "Found 5 files";
}
```

### Resources

```java
@McpResource(
    uri = "server://config",
    name = "Server Configuration",
    description = "Current server configuration",
    mimeType = "application/json"
)
public Map<String, Object> getConfig() {
    return Map.of(
        "version", "1.0.0",
        "features", List.of("tools", "resources", "prompts")
    );
}
```

### Prompts with Parameters

```java
@McpPrompt(
    name = "code_review",
    description = "Generate code review template"
)
public List<PromptMessage> codeReviewPrompt(
    @McpParam(
        description = "Programming language",
        examples = {"Java", "Python", "JavaScript"}
    )
    String language,

    @McpParam(
        description = "Code complexity level",
        examples = {"simple", "moderate", "complex"},
        defaultValue = "moderate",
        required = false
    )
    String complexity
) {
    return List.of(
        new PromptMessage(Role.USER,
            new TextContent("Review this " + language + " code...")
        )
    );
}
```

### Memory Tool

```java
@McpServer(name = "LearningBot", version = "1.0.0")
@McpMemory  // Enable built-in memory tool
public class LearningServer {

    @McpTool(description = "Remember information")
    public String remember(
        @McpParam(description = "Information to remember")
        String info
    ) {
        return "I'll remember: " + info;
    }

    @McpTool(description = "Recall information")
    public String recall(String topic) {
        return "Recalling info about: " + topic;
    }

    public static void main(String[] args) {
        FastMCP.server(LearningServer.class)
            .memoryStore(new InMemoryMemoryStore())  // Optional custom store
            .stdio()
            .run();
    }
}
```

### Todo Tool

```java
@McpServer(name = "TaskManager", version = "1.0.0")
@McpTodo  // Enable built-in todo tool
public class TaskServer {

    public static void main(String[] args) {
        FastMCP.server(TaskServer.class)
            .todoStore(new InMemoryTodoStore())  // Optional custom store
            .streamable()
            .port(3000)
            .run();
    }
}
```

### Planner Tool

```java
@McpServer(name = "PlanningServer", version = "1.0.0")
@McpPlanner  // Enable built-in planner tool
public class PlanningServer {

    public static void main(String[] args) {
        FastMCP.server(PlanningServer.class)
            .planStore(new InMemoryPlanStore())  // Optional custom store
            .streamable()
            .port(3000)
            .run();
    }
}
```

### File Read/Write Tools

```java
@McpServer(name = "FileServer", version = "1.0.0")
@McpFileRead   // Enable file reading tools
@McpFileWrite  // Enable file writing tools
public class FileServer {

    public static void main(String[] args) {
        FastMCP.server(FileServer.class)
            .streamable()
            .port(3000)
            .run();
    }
}
```

---

## Configuration

### Connecting to Claude Desktop

#### HTTP Streamable (Recommended)

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "myserver": {
      "transport": {
        "type": "http_streamable",
        "url": "http://localhost:3000/mcp"
      }
    }
  }
}
```

#### SSE Transport

```json
{
  "mcpServers": {
    "myserver": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:3000/mcp/sse"
      }
    }
  }
}
```

#### STDIO Transport

```json
{
  "mcpServers": {
    "myserver": {
      "command": "java",
      "args": ["-cp", "fastmcp-java.jar", "com.example.MyServer"]
    }
  }
}
```

---

## Available Tools

### Built-in Tool Sets

| Tool Set | Operations |
|----------|------------|
| **memory** | `list`, `read`, `create`, `replace`, `insert`, `delete`, `rename` |
| **todo** | `add`, `list`, `updateStatus`, `updateTask`, `delete`, `clearCompleted`, `get`, `count` |
| **planner** | `createPlan`, `listPlans`, `getPlan`, `addTask`, `addSubtask`, `updateTaskStatus`, `getAllTasks`, `getNextTask`, `deletePlan` |
| **fileread** | `readLines`, `readFile`, `grep`, `getStats` |
| **filewrite** | `writeFile`, `appendFile`, `writeLines`, `deleteFile`, `createDirectory` |

---

## Testing

Run the test suite:

```bash
mvn test
```

Run a specific test:

```bash
mvn test -Dtest=FastMCPTest
```

**Test Coverage**: 168 tests covering all core functionality

---

## Building

```bash
# Build
mvn clean install

# Run example server
mvn exec:java -Dexec.mainClass="com.ultrathink.fastmcp.example.EchoServer"
```

---

## Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [API Documentation](PLAN.md)
- [Roadmap](ROADMAP.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Security Policy](SECURITY.md)

---

## Contributing

FastMCP4J is in active development. Contributions are welcome!

Areas of focus:
- Authentication/authorization
- MCP Providers and transforms
- Storage backends
- Additional transport options

---

## License

MIT License

---

<div align="center">

**Built with ❤️ for the Java community**

</div>
