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

## Status

🚧 **Work in Progress** - v0.1.0 in development

See [PLAN.md](PLAN.md) for implementation details and roadmap.

## Build

```bash
mvn clean install
```

## License

Apache License 2.0
