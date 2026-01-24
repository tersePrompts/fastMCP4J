# fastMCP4J

**A batteries-included framework for building MCP servers in Java**

FastMCP4J brings the simplicity of FastMCP (Python/TypeScript) to Java, reducing boilerplate from 30+ lines to ~10 lines per MCP server.

## Quick Start

```java
@McpServer(name = "Calculator", version = "1.0.0")
public class CalculatorServer {
    @McpTool(description = "Add two numbers")
    public int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        FastMCP.server(CalculatorServer.class).stdio().run();
    }
}
```

## Features

- **Annotation-based API** - Use `@McpTool`, `@McpResource`, `@McpPrompt` instead of verbose builders
- **Auto schema generation** - JSON schemas generated from Java types
- **Multiple transports** - stdio, SSE, streamable HTTP
- **Pure Java 17+** - No Spring, no Jakarta EE, minimal dependencies
- **Lombok-powered** - Terse, immutable data classes

## Comparison: FastMCP4J vs FastMCP (Python)

| Feature | FastMCP (Python) | FastMCP4J (Java) | Status |
|---------|------------------|-------------------|--------|
| **Core Decorators** | `@mcp.tool`, `@mcp.resource`, `@mcp.prompt` | `@McpTool`, `@McpResource`, `@McpPrompt` | ✅ Implemented |
| **Auto Schema Generation** | Yes | Yes | ✅ Implemented |
| **Async Support** | Native `async/await` | `@McpAsync` + Reactor (Mono/Flux) | ✅ Implemented |
| **Multiple Transports** | stdio, SSE, HTTP | stdio, SSE, HTTP_STREAMABLE | 🔄 In Progress |
| **Argument Binding** | Yes | Yes | ✅ Implemented |
| **Response Marshalling** | Yes | Yes | ✅ Implemented |
| **Parameter Validation** | Built-in types + custom | Primitives + POJO | 🔄 In Progress |
| **CLI Tools** | `fastmcp dev`, `fastmcp install` | Not implemented | ⬜ Pending |
| **OpenAPI Generation** | From FastAPI specs | Yes (ServerMeta → OpenAPI 3.0) | ✅ Implemented |
| **Resource Templates** | URI template support | Basic `@McpResource(uri="...")` | 🔄 In Progress |
| **Content Blocks** | Yes | Text-only | 🔄 In Progress |
| **Notifications** | Yes | Yes (logging, progress, resource changes) | ✅ Implemented |
| **Completion/Autocomplete** | Yes | Not implemented | ⬜ Pending |
| **Component Visibility** | Yes | Not implemented | ⬜ Pending |
| **MCP Context Access** | Yes | Not implemented | ⬜ Pending |
| **OAuth/Auth Providers** | Yes | Not implemented | ⬜ Pending |
| **Server Composition** | Yes | Not implemented | ⬜ Pending |
| **Spring Boot Integration** | N/A | Not implemented | ⬜ Pending |

**Legend**: ✅ Implemented | 🔄 In Progress | ⬜ Pending (v0.2+)

### Key Differences

- **Async Model**: Python uses native `async/await`; Java uses Project Reactor (`Mono`/`Flux`) via `@McpAsync`
- **Type System**: Java requires explicit parameter types and `-parameters` compiler flag or `@JsonProperty` annotations
- **Deployment**: Python offers CLI tools (`fastmcp install` for Claude Desktop); Java uses standard Maven/JAR deployment
- **Framework Stack**: FastMCP4J is pure Java (no Spring), intentionally minimal dependencies

## Status

🚧 **Work in Progress** - v0.1.0 in development

See [PLAN.md](PLAN.md) for implementation details and roadmap.

## Build

```bash
mvn clean install
```

## License

Apache License 2.0
