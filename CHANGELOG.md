# Changelog

All notable changes to FastMCP4J will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Pagination support for large result sets
- Progress tracking for long-running operations
- LLM sampling capability

## [0.2.0] - 2026-01-26

### Added
- **Context Access** - Full support for accessing request/client context within tool, resource, and prompt handlers
  - `@McpContext` annotation for parameter injection
  - `Context` interface with comprehensive capability methods
  - Client metadata access (name, version, capabilities)
  - Request context (headers, transport info, session data)
  - Thread-safe context propagation with `ContextImpl.getCurrentContext()`
  - Logging capabilities (debug, info, warning, error)
  - Progress reporting support
  - Session state management (in-memory ConcurrentHashMap)
  - Request and server information access
- **Icons** - Visual icons support for servers, tools, resources, and prompts
  - `icons` field on `@McpServer`, `@McpTool`, `@McpResource`, and `@McpPrompt` annotations
  - Support for HTTPS URLs and data URIs
  - Multiple sizes and themes (light/dark)
  - `Icon` model class with MCP spec compliant format
  - `IconValidator` with security validations (URI schemes, MIME types, sizes, themes)
  - Comprehensive icon string format parser (source:mimeType:sizes:theme)

### Changed
- Updated `ArgumentBinder` to detect and inject Context parameters
- Updated `ToolHandler` to manage Context lifecycle
- Updated `FastMCP` to pass server name to handlers
- `@McpContext` parameters are now hidden from tool schemas

### Fixed
- Thread-local context cleanup for async operations

## [0.1.0] - 2025-12-XX

### Added
- **Core MCP Elements**
  - `@McpServer` annotation for server definition
  - `@McpTool` annotation for exposing methods as callable tools
  - `@McpResource` annotation for serving data/content with URI access
  - `@McpPrompt` annotation for defining LLM interaction templates
  - `@McpParam` annotation for rich parameter metadata (descriptions, examples, constraints)

- **Transport Options**
  - STDIO transport for CLI tools and local agents
  - HTTP SSE (Server-Sent Events) for long-lived connections
  - HTTP Streamable for bidirectional streaming with latest MCP protocol

- **Fluent Builder API**
  - Network configuration (port, MCP URI, base URL)
  - Timeout configuration (request timeout, keep-alive)
  - Server capabilities builder (tools, resources, prompts, logging, completions)
  - Custom store configuration (memory, todo, plan)
  - Instructions configuration

- **Built-in Tools**
  - `@McpMemory` - Persistent storage for cross-session learning
  - `@McpTodo` - Task management with priorities and status
  - `@McpPlanner` - Task decomposition and planning
  - `@McpFileRead` - File reading, grep, and statistics
  - `@McpFileWrite` - File writing and directory operations

- **Advanced Features**
  - Async support with Project Reactor (`@McpAsync`)
  - Pre/post execution hooks (`@McpPreHook`, `@McpPostHook`)
  - Enhanced parameter metadata with examples, constraints, and hints
  - Server capabilities configuration with listChanged support

### Documentation
- README with comprehensive usage examples
- Memory Tool Guide
- Enhanced Parameters Guide
- API Documentation (PLAN.md)

[Unreleased]: https://github.com/ultrathink/fastmcp4j/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/ultrathink/fastmcp4j/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/ultrathink/fastmcp4j/releases/tag/v0.1.0
