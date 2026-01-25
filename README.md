# fastMCP4J

**A batteries-included framework for building MCP servers in Java**

FastMCP4J brings the simplicity of [FastMCP](https://gofastmcp.com) (Python/TypeScript) to Java, reducing boilerplate from 30+ lines to ~10 lines per MCP server.

## Quick Start

```java
@McpServer(name = "Calculator", version = "1.0.0")
public class CalculatorServer {

    @McpTool(description = "Add two numbers with enhanced parameter descriptions")
    public int add(
        @McpParam(
            description = "First number to add",
            examples = {"10", "42", "-5"},
            constraints = "Must be a valid integer"
        )
        int a,

        @McpParam(
            description = "Second number to add",
            examples = {"5", "13", "100"},
            constraints = "Must be a valid integer"
        )
        int b
    ) {
        return a + b;
    }

    public static void main(String[] args) {
        FastMCP.server(CalculatorServer.class)
            .streamable()  // or .stdio() or .sse()
            .port(3000)
            .run();
    }
}
```

## Features

### Core Components ✅
- **Tools** - `@McpTool` with enhanced parameter descriptions via `@McpParam`
- **Resources** - `@McpResource` for exposing data/content
- **Prompts** - `@McpPrompt` for LLM interaction templates
- **Enhanced Parameters** - `@McpParam` with descriptions, examples, constraints, hints, and defaults
- **Memory** - `@McpMemory` for persistent storage and cross-session learning

### Transport & Runtime ✅
- **Multiple transports** - stdio, Server-Sent Events (SSE), HTTP Streamable
- **Async support** - `@McpAsync` with Project Reactor (Mono/Flux)
- **Auto schema generation** - JSON schemas from Java types + enhanced metadata
- **Pure Java 17+** - No Spring, no Jakarta EE, minimal dependencies

## Feature Comparison

### ✅ Implemented (v0.1.0)

| Feature | Description | Implementation |
|---------|-------------|----------------|
| **Tools** | Expose functions as MCP tools | `@McpTool` with auto schema generation |
| **Resources** | Serve content/data | `@McpResource` with MIME type support |
| **Prompts** | LLM interaction templates | `@McpPrompt` with parameterization |
| **Enhanced Parameters** | Rich parameter metadata for LLMs | `@McpParam` (descriptions, examples, constraints, hints, defaults) |
| **Async Support** | Reactive operations | `@McpAsync` with Reactor Mono/Flux |
| **Multiple Transports** | stdio, SSE, HTTP | stdio, SSE, HTTP Streamable |
| **Schema Generation** | Automatic JSON Schema | From Java types + enhanced metadata |
| **Argument Binding** | Auto parameter mapping | Primitives, POJOs, Collections |
| **Response Marshalling** | Auto response conversion | String, JSON, binary, multiple formats |
| **Notifications** | Server→Client updates | Logging, progress, resource changes |

### 🔄 In Progress (v0.2.0)

| Feature | Description | Priority |
|---------|-------------|----------|
| **Context Access** | Access request/client context | High |
| **Icons** | Tool/resource icons | Medium |
| **Pagination** | Large result set handling | High |
| **Progress Tracking** | Long-running operation updates | Medium |
| **Sampling** | LLM completion requests | Low |

### 📋 Roadmap (v0.3.0+)

Based on [FastMCP Python features](https://gofastmcp.com):

#### Advanced Features
- **Background Tasks** - Long-running operations with status tracking
- **Dependencies** - Dependency injection for tools/resources
- **Lifespan Management** - Startup/shutdown hooks
- **Storage Backends** - Persistent storage integration
- **Telemetry** - Metrics, tracing, and observability
- **Versioning** - API versioning support
- **Middleware** - Request/response interceptors

#### Authentication & Authorization
- **Token Verification** - JWT/API key validation
- **OAuth Integration** - OAuth 2.0 flows
- **OIDC Support** - OpenID Connect integration
- **Authorization** - Role-based access control (RBAC)

#### MCP Providers (Server Composition)
- **Local Providers** - Compose multiple MCP servers
- **Filesystem Provider** - Expose filesystem as MCP resources
- **MCP Proxy** - Proxy to remote MCP servers
- **Skills** - Reusable MCP skill packages
- **Custom Providers** - Build custom MCP providers
- **Mounting** - Mount providers at specific paths

#### MCP Transforms
- **Namespace** - Add prefixes to tools/resources
- **Tool Transformation** - Modify tool schemas dynamically
- **Visibility Control** - Show/hide components conditionally
- **Resources as Tools** - Expose resources as callable tools
- **Prompts as Tools** - Convert prompts to tool calls

### ❌ Not Planned

- **OpenAPI to MCP Converter** - Recommended for bootstrapping only, not core framework feature
- **CLI Installation Tools** - Java deployment uses standard Maven/Gradle tooling

### Key Differences from FastMCP Python

- **Async Model**: Python uses `async/await`; Java uses Project Reactor (`Mono`/`Flux`)
- **Type System**: Java requires explicit types; enhanced with `@McpParam` metadata
- **Deployment**: Java uses standard JAR packaging; no special CLI needed
- **Framework Stack**: Pure Java 17+, no heavy frameworks (Spring, Jakarta EE)

## Examples

### Enhanced Parameters with @McpParam

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
            new TextContent("Review this " + language + " code..."))
    );
}
```

### Async Operations

```java
@McpTool(description = "Process large dataset asynchronously")
@McpAsync
public Mono<String> processData(String datasetId) {
    return Mono.fromCallable(() -> {
        // Long-running operation
        return "Processed: " + datasetId;
    }).delayElement(Duration.ofSeconds(5));
}
```

### Memory Tool

```java
@McpServer(name = "LearningBot", version = "1.0.0")
@McpMemory  // Enable memory for persistent storage
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
        // AI can use memory tool to retrieve stored information
        return "Recalling info about: " + topic;
    }

    public static void main(String[] args) {
        FastMCP.server(LearningServer.class)
            .stdio()
            .run();
    }
}
```

The `@McpMemory` annotation enables a memory tool that allows AI agents to persist information across sessions. You can also customize the memory store:

```java
public static void main(String[] args) {
    // Custom memory store with 50MB file size limit
    MemoryStore customStore = new InMemoryMemoryStore("/memories", 50_000_000);

    FastMCP.server(LearningServer.class)
        .memoryStore(customStore)
        .stdio()
        .run();
}
```

### Complete Server Example

See [EchoServer.java](src/test/java/com/ultrathink/fastmcp/example/EchoServer.java) for a comprehensive example demonstrating all features.

## Status

✅ **v0.1.0** - Core features implemented
- Tools, Resources, Prompts
- Enhanced parameter descriptions (@McpParam)
- Async support with Reactor
- Multiple transports (stdio, SSE, HTTP Streamable)
- Comprehensive test coverage (27/27 tests passing)

🚧 **v0.2.0** - In planning
- Context access
- Pagination
- Icons
- Progress tracking

See [Feature Comparison](#feature-comparison) for detailed roadmap.

## Building

```bash
# Build
mvn clean install

# Run tests
mvn test

# Run example server
mvn exec:java -Dexec.mainClass="com.ultrathink.fastmcp.example.EchoServer"
```

## Documentation

- [Memory Tool Guide](docs/memory-tool.md)
- [Enhanced Parameters Guide](docs/enhanced-parameters.md)
- [API Documentation](PLAN.md) (Implementation details)

## Contributing

FastMCP4J is in active development. Contributions are welcome!

Areas of focus:
- Context access and middleware
- Authentication/authorization
- MCP Providers and transforms
- Storage backends

## License

Apache License 2.0
