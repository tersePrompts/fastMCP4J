# Sync Tool Threadpool Feature

## Purpose

Prevent synchronous tool methods from blocking the reactive event loop. When a tool is marked as synchronous (no `@McpAsync` annotation), its execution could block the reactive pipeline if it performs CPU-intensive or I/O operations. This feature automatically wraps synchronous tool execution in a bounded elastic threadpool.

## Use Case

For CPU-intensive or I/O blocking tools written as sync methods:

```java
// Sync tool that would block the event loop
@McpTool(description = "Process large data")
public String processData(String data) {
    // CPU-intensive operation
    return heavyComputation(data);
}
```

Without the threadpool, this would block the reactive event loop during execution. With the threadpool enabled, the sync method executes on a separate thread.

## Design

Sync tool detection:
- A tool is considered sync if it lacks `@McpAsync` annotation
- The method return type is not `Mono<?>`

Sync execution wrapping:
```java
Mono.fromCallable(() -> method.invoke(instance, args))
    .subscribeOn(Schedulers.boundedElastic())
```

This approach:
- Uses Reactor's bounded elastic scheduler for blocking operations
- Maintains the reactive programming model
- Handles exceptions from wrapped calls

## Configuration

The threadpool is enabled by default. Optional threadpool size configuration can be set via the FastMCP builder:

```java
FastMCP.server(MyServer.class)
    .syncToolThreadpoolSize(16)  // Optional: configure threadpool size
    .build();
```

If not configured, uses Reactor's default bounded elastic scheduler (typically 2x CPU cores).

## Implementation Notes

- The threadpool is only used for sync tools (no `@McpAsync`)
- Async tools (with `@McpAsync`) are not affected - they manage their own scheduling
- Exceptions from sync tools are properly propagated through the reactive pipeline
- The bounded elastic scheduler is designed for blocking I/O operations
