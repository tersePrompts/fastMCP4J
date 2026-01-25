# FastMCP4J Roadmap

## Version 0.2.0 - In Progress

Target: Q1 2026

### Feature: Context Access
**Priority**: High
**Status**: Not Started
**Assignee**: Unassigned

#### Description
Provide access to request/client context within tool, resource, and prompt handlers.

#### Requirements
- Access to client metadata (name, version, capabilities)
- Request context (headers, transport info, session data)
- Ability to retrieve context via annotation or method parameter
- Thread-safe context propagation in async operations

#### Proposed API
```java
@McpTool(description = "Get current user info")
public String getUserInfo(@McpContext RequestContext context) {
    String clientId = context.getClientId();
    Map<String, String> headers = context.getHeaders();
    return "Client: " + clientId;
}

// Alternative: ThreadLocal context
@McpTool(description = "Get session data")
public String getSession() {
    McpContext context = McpContext.current();
    return context.getSessionId();
}
```

#### Implementation Tasks
- [ ] Create `McpContext` interface
- [ ] Create `@McpContext` annotation for parameter injection
- [ ] Implement context propagation in `ArgumentBinder`
- [ ] Add ThreadLocal context storage for async operations
- [ ] Implement Reactor context integration
- [ ] Add tests for sync and async context access
- [ ] Document context API in README

#### Dependencies
- None

#### Related Issues
- None

---

### Feature: Icons
**Priority**: Medium
**Status**: Not Started
**Assignee**: Unassigned

#### Description
Support icons for tools and resources to improve UI/UX in MCP clients.

#### Requirements
- Support icon URLs (http/https)
- Support data URIs for inline icons
- Support emoji icons (Unicode)
- Icons optional on all components
- Follow MCP spec icon format

#### Proposed API
```java
@McpTool(
    name = "search",
    description = "Search files",
    icon = "🔍"  // Emoji
)
public String search(String query) { ... }

@McpTool(
    name = "calculate",
    description = "Calculate",
    icon = "https://example.com/calc-icon.png"  // URL
)
public double calculate(String expression) { ... }

@McpResource(
    uri = "config://app",
    name = "Configuration",
    icon = "data:image/svg+xml;base64,..."  // Data URI
)
public String getConfig() { ... }
```

#### Implementation Tasks
- [ ] Add `icon` field to `@McpTool` annotation
- [ ] Add `icon` field to `@McpResource` annotation
- [ ] Add `icon` field to `@McpPrompt` annotation
- [ ] Update `ToolMeta`, `ResourceMeta`, `PromptMeta` models
- [ ] Update schema builders to include icon field
- [ ] Add icon validation (URL format, emoji, data URI)
- [ ] Add tests for different icon types
- [ ] Update EchoServer with icon examples
- [ ] Document icon usage in README

#### Dependencies
- None

#### Related Issues
- None

---

### Feature: Pagination
**Priority**: High
**Status**: Not Started
**Assignee**: Unassigned

#### Description
Handle large result sets with cursor-based pagination for tools and resources.

#### Requirements
- Support cursor-based pagination (MCP spec compliant)
- Support offset/limit pagination (convenience)
- Automatic pagination for large collections
- Configurable page size limits
- Return next cursor in responses

#### Proposed API
```java
// Cursor-based pagination
@McpTool(description = "List all users with pagination")
public PaginatedResult<User> listUsers(
    @McpParam(description = "Pagination cursor", required = false)
    String cursor,

    @McpParam(description = "Page size", defaultValue = "50", required = false)
    int limit
) {
    List<User> users = fetchUsers(cursor, limit);
    String nextCursor = hasMore() ? generateCursor() : null;
    return PaginatedResult.of(users, nextCursor);
}

// Automatic pagination via annotation
@McpTool(description = "Get all items")
@McpPaginated(pageSize = 100, maxResults = 1000)
public List<Item> getAllItems() {
    return itemRepository.findAll();  // Auto-paginated
}
```

#### Implementation Tasks
- [ ] Create `PaginatedResult<T>` wrapper class
- [ ] Create `@McpPaginated` annotation
- [ ] Implement cursor generation and parsing
- [ ] Update `ResponseMarshaller` to handle pagination
- [ ] Add pagination metadata to tool schemas
- [ ] Implement automatic pagination for List returns
- [ ] Add cursor encoding/decoding utilities
- [ ] Add tests for paginated responses
- [ ] Document pagination patterns in README

#### Dependencies
- None

#### Related Issues
- None

---

### Feature: Progress Tracking
**Priority**: Medium
**Status**: Not Started
**Assignee**: Unassigned

#### Description
Track and report progress for long-running operations.

#### Requirements
- Send progress notifications to clients
- Support progress percentage (0-100)
- Support progress messages/status
- Compatible with async operations
- Support cancellation (future)

#### Proposed API
```java
@McpTool(description = "Process large file")
@McpAsync
public Mono<String> processFile(
    String fileId,
    @McpProgress ProgressReporter progress
) {
    return Mono.fromCallable(() -> {
        progress.report(0, "Starting processing...");

        // Step 1
        readFile(fileId);
        progress.report(25, "File read complete");

        // Step 2
        transformData();
        progress.report(50, "Data transformed");

        // Step 3
        validate();
        progress.report(75, "Validation complete");

        // Step 4
        save();
        progress.report(100, "Processing complete");

        return "Success";
    });
}
```

#### Implementation Tasks
- [ ] Create `ProgressReporter` interface
- [ ] Create `@McpProgress` annotation for parameter injection
- [ ] Implement progress notification sender
- [ ] Integrate with MCP notification protocol
- [ ] Add progress tracking to async tool handlers
- [ ] Handle progress in sync vs async contexts
- [ ] Add tests for progress notifications
- [ ] Update EchoServer with progress example
- [ ] Document progress tracking in README

#### Dependencies
- Context Access (for notification routing)

#### Related Issues
- None

---

### Feature: Sampling
**Priority**: Low
**Status**: Not Started
**Assignee**: Unassigned

#### Description
Request LLM completions from the client (MCP sampling capability).

#### Requirements
- Send sampling requests to client
- Support different sampling parameters
- Handle streaming responses
- Support model selection
- Follow MCP sampling spec

#### Proposed API
```java
@McpTool(description = "Generate SQL from natural language")
public String generateSql(
    String naturalLanguageQuery,
    @McpSampler LLMSampler sampler
) {
    String prompt = "Convert to SQL: " + naturalLanguageQuery;

    SamplingRequest request = SamplingRequest.builder()
        .prompt(prompt)
        .maxTokens(200)
        .temperature(0.7)
        .build();

    String completion = sampler.complete(request);
    return completion;
}

// Streaming variant
@McpTool(description = "Generate code with streaming")
@McpAsync
public Flux<String> generateCode(
    String description,
    @McpSampler LLMSampler sampler
) {
    return sampler.completeStream(
        SamplingRequest.builder()
            .prompt("Generate code for: " + description)
            .maxTokens(500)
            .build()
    );
}
```

#### Implementation Tasks
- [ ] Create `LLMSampler` interface
- [ ] Create `@McpSampler` annotation
- [ ] Create `SamplingRequest` and `SamplingResponse` models
- [ ] Implement MCP sampling protocol
- [ ] Support synchronous completions
- [ ] Support streaming completions (Flux)
- [ ] Add model selection support
- [ ] Handle sampling errors gracefully
- [ ] Add tests for sampling
- [ ] Document sampling usage in README

#### Dependencies
- Context Access (for client communication)

#### Related Issues
- None

---

## Version 0.3.0 - Planning

Target: Q2 2026

### Planned Features
- Background Tasks
- Dependencies (Dependency Injection)
- Lifespan Management
- Storage Backends
- Telemetry
- Versioning
- Middleware

### Authentication & Authorization
- Token Verification
- OAuth Integration
- OIDC Support
- Authorization (RBAC)

### MCP Providers
- Local Providers
- Filesystem Provider
- MCP Proxy
- Skills
- Custom Providers
- Mounting

### MCP Transforms
- Namespace
- Tool Transformation
- Visibility Control
- Resources as Tools
- Prompts as Tools

---

## Contributing

To work on any of these features:

1. Check the feature status in this document
2. Create a feature branch: `feature/context-access`, `feature/pagination`, etc.
3. Update the task checklist as you progress
4. Submit a PR when ready
5. Update this roadmap with completion status

## Notes

- All features follow MCP specification where applicable
- Maintain backward compatibility within major versions
- Add comprehensive tests for all features
- Document all public APIs in README.md
- Update EchoServer with examples for new features
