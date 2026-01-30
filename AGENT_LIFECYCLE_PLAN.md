# Agent Bundle Core Integration Plan

## Overview
Integration of 36 features from agent-bundle-core into FastMCP4J, extending the existing annotation-driven framework while maintaining backward compatibility.

## Package Structure
```
com.ultrathink.fastmcp.agent/
├── lifecycle/          # Session lifecycle management
├── tenancy/           # Multi-tenancy support
├── memory/            # Enhanced memory system
├── state/             # File state service
├── transform/         # Response transformation
├── cache/             # Cache layer
└── spi/               # SPI definitions
```

## Features to Implement (36 total)

### Core Architecture (3)
1. Transport Agnosticism - Single Tool interface
2. Multi-Tenancy - {tenant}/{user}/{namespace}/ path prefixing
3. Session Lifecycle - Bootstrap, expiry, cleanup

### Hook System (7)
4. Pre-Hooks (PRE_TOOL_USE) - DENY/MODIFY/ALLOW
5. Post-Hooks (POST_TOOL_USE) - Result modification
6. Session Lifecycle Hooks - SESSION_START, SESSION_END
7. Hook Matching - EXACT, PREFIX, CONTAINS, REGEX
8. Hook Priority - Numeric ordering
9. Hook Sources - Harness, common, bundle (JSON)
10. Java Hooks - registerJavaHook()

### Session Management (5)
11. Session Bootstrap - tenantId, userId, mcpSessionId
12. Session Lookup - by mcpSessionId or conversationId
13. Domain Context - domain-{persona}.json loading
14. Active Bundle Tracking - setActiveBundle()
15. Session Store SPI - Pluggable persistence

### Tool System (5)
16. Tool Interface - getName, getDescription, getInputSchema, handle
17. ToolContext Accessors - stringArg, intArg, boolArg
18. ToolResult Factories - success(), json(), error()
19. Tool Registration - ToolRegistrar SPI
20. Tool Lifecycle - initialize(), isEnabled(), getOrder(), supportsHooks()

### File State Service (4)
21. StateStore Interface - write, read, exists, delete, list, replace
22. Write Modes - OVERWRITE, APPEND, CREATE_ONLY
23. Session Binding - forSession(session) for isolation
24. Path Traversal Protection - resolveSafe() validation

### Memory System (4)
25. Memory Types - EPISODIC, SEMANTIC, PROCEDURAL, WORKING
26. Memory Scopes - SESSION, USER, TENANT, GLOBAL
27. Memory Store SPI - reader(), writer(), admin(), capabilities()
28. Memory Query - Filter by type, scope, maxResults

### Response Transformation (3)
29. Transformation Strategy SPI - JavaScript-based (GraalJS)
30. URL Pattern Matching - Auto-select by URL (GLOB/REGEX)
31. Transform Config Index - JSON-based configuration

### Cache Layer (3)
32. CacheProvider Interface - get, put (TTL), invalidate, clear
33. In-Memory Cache - Default with TTL support
34. Cache Registry - Provider registration

### Infrastructure (2)
35. Cleanup Scheduler - Session expiration + hook firing
36. Schema Extraction - Compile-time safe from POJOs

## Implementation Phases

### Phase 1: Foundation (Priority)
- Package structure
- SPI interfaces (AgentProvider, SessionInitializer)
- TenantResolverRegistry with default resolver
- Extend ContextImpl with tenant methods

### Phase 2: Session Lifecycle
- SessionLifecycle manager
- @McpSessionLifecycle annotation
- Extend HookManager for session hooks
- Session cleanup task

### Phase 3: Enhanced Hooks
- HookType enum (PRE_TOOL_USE, POST_TOOL_USE, SESSION_START, SESSION_END)
- Extend HookManager
- HookChain and HookExecutor
- Hook priority and async execution

### Phase 4: Memory System
- MemoryType enum and EnhancedMemoryStore
- Memory type-specific stores
- @McpMemoryType annotation
- Integrate with MemoryTool

### Phase 5: File State & Caching
- FileStateService SPI
- SessionBoundState implementation
- CacheProvider SPI
- @McpCached annotation

### Phase 6: Response Transformation
- ResponseTransformer SPI
- JsTransformer with GraalVM
- @McpTransform annotation
- ToolHandler integration

## Backward Compatibility
- All features are opt-in
- Default behavior unchanged when features not configured
- Existing @McpPreHook/@McpPostHook continue to work
- Gradual migration path

## Files to Extend
- FastMCP.java - Add builder methods for agent features
- HookManager.java - Add lifecycle hooks
- ContextImpl.java - Add tenant context
- ToolHandler.java - Add caching/transformation
- AnnotationScanner.java - Scan new annotations
