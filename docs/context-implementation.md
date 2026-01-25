# Context Access Feature Implementation

## Overview

Context Access allows MCP tools, resources, and prompts to access request/client context and MCP capabilities within their handlers. Each MCP request receives a new `Context` object that provides:

- **Logging**: Send debug, info, warning, and error messages back to client
- **Progress Reporting**: Update clients on progress of long-running operations
- **Resource Access**: List and read data from resources registered with server
- **Prompt Access**: List and retrieve prompts registered with server
- **Session State**: Store data that persists across requests within an MCP session
- **Request Information**: Access metadata about the current request
- **Transport**: Get the transport type (stdio, sse, streamable-http)
- **Server Info**: Access server metadata

## Context Scoping

Context is **scoped to a single request**. Each MCP request receives a new context instance. State or data set in one request will not be available in subsequent requests (except for session state, which persists across requests within the same MCP session).

## Usage

### Method 1: Using @McpContext Annotation (Recommended)

The preferred way to access context is using the `@McpContext` annotation:

```java
import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;

@McpServer(name = "My Server")
public class MyServer {
    
    @McpTool(description = "Process file with logging")
    public String processFile(String path, @McpContext Context ctx) {
        ctx.info("Processing: " + path);
        // ... processing logic
        ctx.info("Processing complete!");
        return "Done";
    }
}
```

### Method 2: Using Type Hint (Legacy)

For backwards compatibility, you can also use just the `Context` type:

```java
@McpTool(description = "Process file")
public String processFile(String path, Context ctx) {
    ctx.info("Processing: " + path);
    return "Done";
}
```

Both methods work identically. The parameter name doesn't matter—only the `@McpContext` annotation or `Context` type is important.

### Method 3: Using getCurrentContext()

For code nested deeper within function calls where passing context through parameters is inconvenient:

```java
@McpTool(description = "Analyze dataset")
public String analyzeDataset(String datasetName) {
    // Call utility function that uses context internally
    return processDataset(datasetName);
}

// Utility function not in tool handler
private String processDataset(String datasetName) {
    Context ctx = ContextImpl.getCurrentContext();
    ctx.info("Analyzing dataset: " + datasetName);
    // ... processing logic
    return "Analysis complete";
}
```

**Important Notes:**
- `getCurrentContext()` can only be called within the context of a server request
- Calling it outside of a request will throw an `IllegalStateException`
- Context methods are synchronous, so they can be used in both sync and async handlers

## Context Capabilities

### Logging

Send log messages to the MCP client:

```java
ctx.debug("Detailed diagnostic information");
ctx.info("General informational message");
ctx.warning("Warning about potential issue");
ctx.error("Error that occurred during processing");
```

### Progress Reporting

Report progress for long-running operations:

```java
@McpTool(description = "Process large dataset")
public String processDataset(String dataset, @McpContext Context ctx) {
    int totalItems = 1000;
    
    for (int i = 0; i < totalItems; i++) {
        // Process item
        processItem(i);
        
        // Report progress every 100 items
        if (i % 100 == 0) {
            ctx.reportProgress(i, totalItems, "Processing item " + i);
        }
    }
    
    ctx.reportProgress(totalItems, totalItems, "Complete!");
    return "Processed " + totalItems + " items";
}
```

### Session State

Store and retrieve data that persists across requests within the same MCP session:

```java
@McpTool(description = "Increment counter")
public int incrementCounter(@McpContext Context ctx) {
    Integer count = (Integer) ctx.getState("counter");
    int newCount = (count == null) ? 1 : count + 1;
    ctx.setState("counter", newCount);
    return newCount;
}

@McpTool(description = "Get counter value")
public int getCounter(@McpContext Context ctx) {
    return (Integer) ctx.getState("counter") != null ? 
        (Integer) ctx.getState("counter") : 0;
}
```

**Important:**
- Each client session has its own isolated state
- Session state is in-memory (suitable for single-server deployments)
- For distributed deployments, a custom storage backend can be configured

### Request Information

Access request metadata:

```java
@McpTool(description = "Get request info")
public Map<String, Object> getRequestInfo(@McpContext Context ctx) {
    return Map.of(
        "requestId", ctx.getRequestId(),
        "clientId", ctx.getClientId() != null ? ctx.getClientId() : "Unknown",
        "sessionId", ctx.getSessionId(),
        "transport", ctx.getTransport(),
        "serverName", ctx.getServerName()
    );
}
```

### Server Information

Access server metadata:

```java
@McpTool(description = "Get server info")
public String getServerInfo(@McpContext Context ctx) {
    return "Server: " + ctx.getServerName() + 
           ", Transport: " + ctx.getTransport();
}
```

## Async Context Usage

Context works seamlessly with async tools:

```java
import reactor.core.publisher.Mono;

@McpTool(description = "Process data asynchronously")
@McpAsync
public Mono<String> processAsync(String input, @McpContext Context ctx) {
    return Mono.fromRunnable(() -> {
        ctx.info("Starting async processing");
    })
    .then(Mono.delay(Duration.ofMillis(100)))
    .flatMap(x -> {
        ctx.reportProgress(33, 100, "Phase 1 complete");
        return Mono.just(x);
    })
    .flatMap(x -> Mono.delay(Duration.ofMillis(100)))
    .flatMap(x -> {
        ctx.reportProgress(66, 100, "Phase 2 complete");
        return Mono.just(x);
    })
    .flatMap(x -> Mono.delay(Duration.ofMillis(100)))
    .map(x -> {
        ctx.reportProgress(100, 100, "Complete");
        ctx.info("Async processing finished");
        return "Processed: " + input;
    });
}
```

## Resource and Prompt Context

Context works with resources and prompts as well:

```java
@McpResource(uri = "config://server", name = "Config")
public String getConfig(@McpContext Context ctx) {
    ctx.debug("Reading configuration");
    // ... read config
    return configContent;
}

@McpPrompt(description = "Analysis prompt")
public String analysisPrompt(String dataset, @McpContext Context ctx) {
    ctx.info("Creating prompt for: " + dataset);
    return "Analyze dataset: " + dataset;
}
```

## Complete Example

See `src/test/java/com/ultrathink/fastmcp/example/ContextExampleServer.java` for a comprehensive example demonstrating all context capabilities.

## Implementation Details

### Files

- `context/Context.java` - Context interface with all capability methods
- `context/ContextImpl.java` - Concrete implementation with ThreadLocal storage
- `context/McpContext.java` - Annotation for method parameter injection
- `adapter/ArgumentBinder.java` - Updated to support Context injection
- `adapter/ToolHandler.java` - Updated to manage Context lifecycle

### Context Lifecycle

1. **Creation**: When a tool/resource/prompt is invoked, `ToolHandler` creates a new `ContextImpl`
2. **Setup**: Context is set in `ThreadLocal` via `ContextImpl.setCurrentContext()`
3. **Injection**: `ArgumentBinder` detects `@McpContext` or `Context` type and injects the current context
4. **Execution**: Handler method executes with full access to Context capabilities
5. **Cleanup**: After execution completes, `ContextImpl.clearCurrentContext()` removes from ThreadLocal

### Session State Storage

Session state is stored in a `ConcurrentHashMap<String, Map<String, Object>>` keyed by session ID. Each session has its own isolated state map.

For production deployments requiring distributed state, the storage can be extended to use:
- Redis
- DynamoDB
- MongoDB
- Any `AsyncKeyValue` compatible backend

### Notification Integration

Context logging and progress reporting integrate with the existing `NotificationSender`:
- Log messages are sent via `NotificationSender.sendLog()`
- Progress updates are sent via `NotificationSender.progress()`
- Resource changes are sent via `NotificationSender.resourceChanged()`

## Testing

Run the example server to see Context in action:

```bash
# Run the example server
java -cp target/fastmcp-java.jar \
  com.ultrathink.fastmcp.example.ContextExampleServer

# Connect with an MCP client to test
# Call tools, observe logs and progress
# Test session state across multiple tool calls
```

## Specification Compliance

This implementation follows the MCP Context specification as documented in:
https://gofastmcp.com/servers/context

## Future Enhancements

Planned additions:
- **LLM Sampling**: Request LLM completions from client
- **User Elicitation**: Request structured input from users
- **Resource List**: List all available resources
- **Prompt List**: List and retrieve all available prompts
- **Custom Storage**: Pluggable storage backends for session state
- **Change Notifications**: Manual notification triggers for component changes
