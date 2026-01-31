# MCP Client Configuration Examples

This directory contains sample MCP client configurations for connecting to FastMCP4J servers.

## EchoServer Configuration

The `echo-server-config.json` file contains a sample configuration for connecting to the EchoServer demo.

### Prerequisites

1. Start the EchoServer:
   ```bash
   mvn exec:java -Dexec.mainClass="io.github.terseprompts.fastmcp.example.EchoServer"
   ```

2. The server will start on `http://localhost:3002/mcp`

### Claude Desktop Setup

#### macOS
Config location: `~/Library/Application Support/Claude/claude_desktop_config.json`

#### Windows
Config location: `%APPDATA%\Claude\claude_desktop_config.json`

#### Linux
Config location: `~/.config/Claude/claude_desktop_config.json`

### Configuration Options

#### HTTP Streamable (Recommended)

```json
{
  "mcpServers": {
    "echo": {
      "transport": {
        "type": "http_streamable",
        "url": "http://localhost:3002/mcp"
      }
    }
  }
}
```

#### SSE Transport

If you modified EchoServer to use SSE:

```json
{
  "mcpServers": {
    "echo": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:3002/mcp/sse"
      }
    }
  }
}
```

#### STDIO Transport

For stdio transport (requires JAR on classpath):

```json
{
  "mcpServers": {
    "echo": {
      "command": "java",
      "args": [
        "-cp",
        "target/fastmcp-java-0.1.0-SNAPSHOT.jar",
        "io.github.terseprompts.fastmcp.example.EchoServer"
      ]
    }
  }
}
```

### Available Tools

Once connected, these tools are available:

| Tool | Description |
|------|-------------|
| `echo` | Echo message with timestamp |
| `calculate` | Arithmetic operations |
| `processWithProgress` | Task with progress reporting |
| `asyncTask` | Async task execution |
| `memory` | Persistent storage |
| `todo` | Task management |
| `planner` | Hierarchical planning |
| `fileread` | File reading operations |
| `filewrite` | File writing operations |

### Testing

After configuration, restart Claude Desktop and try:

```
Use the echo tool to say "Hello, FastMCP4J!"
```

Expected response:
```
[17:30:45] Echo: Hello, FastMCP4J!
```
