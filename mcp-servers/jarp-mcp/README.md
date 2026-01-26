# JARP-MCP for fastMCP4J

This directory contains [jarp-mcp](https://www.npmjs.com/package/jarp-mcp) - an MCP server that gives AI agents instant access to decompiled Java code from Maven/Gradle dependencies.

## What it does

When you're working with Java code that depends on libraries, Claude can now:
- Decompile any Java class from JAR files in your dependencies
- Analyze class structure, methods, and fields
- Understand internal APIs without manual decompilation

## Installation

Already installed. Run `npm install` again if needed:
```bash
cd mcp-servers/jarp-mcp
npm install
```

## Usage with Claude Desktop

Add to your Claude Desktop config (`%APPDATA%\Claude\claude_desktop_config.json` on Windows):

```json
{
  "mcpServers": {
    "jarp-mcp": {
      "command": "node",
      "args": ["C:\\Users\\av201\\workspace\\fastMCP4J\\mcp-servers\\jarp-mcp\\node_modules\\jarp-mcp\\dist\\index.js", "start"],
      "cwd": "C:\\Users\\av201\\workspace\\fastMCP4J"
    }
  }
}
```

## Usage with Claude Code CLI

For Claude Code CLI, MCP servers are configured globally. Add to your user Claude config or use the `mcp-config.json` in this directory as a reference.

The config file `mcp-config.json` contains the server configuration - you can copy its contents into your global Claude MCP settings.

## Available Tools

Once configured, Claude will have access to:
- `scan_dependencies` - Scan Maven/Gradle dependencies and build class index
- `decompile_class` - Decompile a Java class from JAR files
- `analyze_class` - Analyze class structure, methods, fields, etc.

## Example Workflow

```
User: "How does JpaRepository's saveAll method work?"
Claude: [uses jarp-mcp to decompile JpaRepository]
       "The actual signature is: <S extends T> List<S> saveAll(Iterable<S> entities)
        Here's how it works..."
```

## Learn More

- [jarp-mcp on npm](https://www.npmjs.com/package/jarp-mcp)
- [CFR Decompiler](https://www.benf.org/other/cfr/)
