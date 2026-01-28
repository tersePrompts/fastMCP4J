# Changelog

All notable changes to FastMCP4J will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Pagination support for large result sets
- Progress tracking for long-running operations
- LLM sampling capability

## [0.3.0-beta] - 2026-01-28

### Added
- **JSON Schema 2020-12 Compliance** - Full compliance with latest JSON Schema specification
  - Added `$schema` declaration to all generated schemas
  - Removed non-standard keywords (`examples`, `constraints`, `hints` as separate fields)
  - All metadata now embedded in `description` field for LLM visibility

### Fixed
- **Security Vulnerabilities**
  - Fixed GHSA-qh8g-58pp-2wxh - Jetty HTTP URI parsing vulnerability (upgraded to 11.0.26)
  - Fixed CVE-2025-5115 - Jetty HTTP/2 "MadeYouReset" DoS vulnerability
  - Upgraded SLF4J from 2.0.16 to 2.0.17

### Changed
- `required` attribute now only at schema root level (JSON Schema 2020-12 compliant)
- Removed property-level `required` field which was non-standard
- All parameter examples, constraints, and hints now in description text

### Technical Details
- Jackson 2.18.2 (no known CVEs)
- Lombok 1.18.36 (no known CVEs)
- JUnit 5.11.4 (no known CVEs)
- MCP SDK 0.17.2
- Jetty 11.0.26 (secure)

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
- Architecture Documentation (ARCHITECTURE.md)

[Unreleased]: https://github.com/tersePrompts/fastMCP4J/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/tersePrompts/fastMCP4J/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/tersePrompts/fastMCP4J/releases/tag/v0.1.0
