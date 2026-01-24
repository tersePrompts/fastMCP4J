# FastMCP4J Implementation Plan

## Motivation

**Problem**: The official MCP Java SDK requires 30+ lines of verbose builder code per tool. Enterprise Java developers need a FastAPI/FastMCP-style framework with decorator-based APIs, auto schema generation, and zero boilerplate.

**Goal**: Enable production-ready MCP servers in 10 lines of code. Make Java a first-class citizen in the MCP ecosystem.

**Target Users**: Enterprise Java shops building AI agents, microservice developers adding MCP capabilities, backend engineers wrapping existing business logic as MCP tools.

**Success Criteria**: User writes `@McpTool`, runs `main()`, gets working server. No configuration files, no XML, no magic.

---

## Repository Rules

**Code Style**:
- Terse, direct code - no verbose naming
- Lombok everywhere - use `@Data`, `@Builder`, `@Value`, `@Slf4j`
- No comments unless logic is genuinely non-obvious
- Prefer composition over inheritance
- Immutable by default (records, `@Value`)

**Architecture**:
- Pure Java 17+ (no Spring, no Jakarta EE)
- Stateless (no session management, no state machines)
- Minimal scaffolding - no abstract base classes unless necessary
- Each public class has corresponding test class

**Logging & Observability**:
- Minimal logging (errors only, no debug/trace)
- No telemetry, no metrics (defer to v0.2+)
- Fail fast with clear exception messages

**Testing**:
- Every feature file → corresponding test file
- JUnit 5, no mocking framework (use real instances)
- Test names: `testMethodName_Scenario_ExpectedBehavior()`

---

## Scope: MVP (v0.1)

### In Scope
1. Annotations: `@McpServer`, `@McpTool`, `@McpResource`, `@McpPrompt`
2. Auto schema generation from Java types
3. Support stdio, SSE, streamable HTTP transports
4. Sync and async handlers
5. Stateless request/response handling

### Out of Scope (Future)
- OAuth, auth providers
- OpenAPI generation
- CLI tooling
- Server composition
- Spring Boot integration

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Code                               │
│                                                                 │
│   @McpServer(name="calc", version="1.0")                       │
│   class CalcServer {                                           │
│       @McpTool(description="Add")                              │
│       int add(int a, int b) { return a + b; }                  │
│   }                                                            │
│                                                                 │
│   FastMCP.server(CalcServer.class).stdio().run();              │
└────────────────────────────────┬────────────────────────────────┘
                                 │
        ┌────────────────────────┴─────────────────────────┐
        ▼                                                  ▼
┌──────────────────┐                             ┌──────────────────┐
│ AnnotationScanner│                             │ SchemaGenerator  │
│                  │                             │                  │
│ • Scan class     │                             │ • Method → JSON  │
│ • Find @McpTool  │────────── Metadata ────────>│ • POJO → schema  │
│ • Validate       │                             │ • Cache schemas  │
└────────┬─────────┘                             └────────┬─────────┘
         │                                                │
         │                                                │
         ▼                                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ToolHandlerAdapter                        │
│                                                                 │
│  Method reflection ─> Argument binding ─> Invoke ─> Marshal    │
│                                                                 │
│  Map<String, Object> ──────────────────────────> CallToolResult│
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                          FastMCP                                │
│                                                                 │
│  • Orchestrates scanning + schema + adapter                    │
│  • Builds McpServer via official SDK                           │
│  • Transport selection (stdio, SSE, streamable)                │
│  • Run loop and graceful shutdown                              │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Official MCP SDK (io.modelcontextprotocol)    │
│                                                                 │
│  McpServer.async() ─> AsyncToolSpecification ─> Tool execution │
│  StdioServerTransportProvider, HttpServletSseProvider, etc.    │
└─────────────────────────────────────────────────────────────────┘
```

**Data Flow**:
1. User writes annotated class
2. FastMCP scans annotations → metadata
3. SchemaGenerator creates JSON schemas from method signatures
4. ToolHandlerAdapter wraps methods as BiFunction handlers
5. FastMCP delegates to official SDK with generated config
6. Transport layer (stdio/SSE/HTTP) handles client communication

---

## Package Structure

```
io.github.fastmcp/
├── annotations/      # CHUNK 1 (can be done first, standalone)
│   ├── McpServer.java
│   ├── McpTool.java
│   ├── McpResource.java
│   ├── McpPrompt.java
│   └── McpAsync.java
│
├── model/           # CHUNK 2 (depends on annotations, can parallel with chunk 3)
│   ├── ServerMeta.java    # Lombok @Value for server metadata
│   ├── ToolMeta.java      # Lombok @Value for tool metadata
│   ├── ResourceMeta.java  # Lombok @Value for resource metadata
│   └── PromptMeta.java    # Lombok @Value for prompt metadata
│
├── scanner/         # CHUNK 3 (depends on annotations + model)
│   ├── AnnotationScanner.java
│   └── ValidationException.java
│
├── schema/          # CHUNK 4 (depends on model, can parallel with chunk 5)
│   ├── SchemaGenerator.java
│   └── SchemaCache.java
│
├── adapter/         # CHUNK 5 (depends on model + schema)
│   ├── ToolHandler.java
│   ├── ResourceHandler.java
│   ├── PromptHandler.java
│   ├── ArgumentBinder.java
│   └── ResponseMarshaller.java
│
├── core/            # CHUNK 6 (depends on all above)
│   ├── FastMCP.java
│   └── TransportType.java
│
└── exception/       # CHUNK 0 (can be done anytime, no deps)
    └── FastMcpException.java
```

---

## Work Breakdown (Parallelizable Chunks)

### CHUNK 0: Exceptions (No dependencies)
**Files**: `exception/FastMcpException.java`
**Test**: `exception/FastMcpExceptionTest.java`

```java
public class FastMcpException extends RuntimeException {
    public FastMcpException(String message) { super(message); }
    public FastMcpException(String message, Throwable cause) { super(message, cause); }
}
```

**Test cases**:
- testConstructorWithMessage
- testConstructorWithCause

---

### CHUNK 1: Annotations (No dependencies)
**Can start immediately, required by all other chunks**

**Files**:
- `annotations/McpServer.java`
- `annotations/McpTool.java`
- `annotations/McpResource.java`
- `annotations/McpPrompt.java`
- `annotations/McpAsync.java`
- `annotations/AnnotationsTest.java` (test)

**Spec**:
```java
@Retention(RUNTIME) @Target(TYPE)
public @interface McpServer {
    String name();
    String version() default "1.0.0";
    String instructions() default "";
}

@Retention(RUNTIME) @Target(METHOD)
public @interface McpTool {
    String name() default "";  // defaults to method name
    String description();
}

@Retention(RUNTIME) @Target(METHOD)
public @interface McpResource {
    String uri();
    String name() default "";
    String description() default "";
    String mimeType() default "text/plain";
}

@Retention(RUNTIME) @Target(METHOD)
public @interface McpPrompt {
    String name() default "";
    String description() default "";
}

@Retention(RUNTIME) @Target(METHOD)
public @interface McpAsync {}  // marker for Mono<?> return
```

**Tests**:
- testAnnotationsAreRuntimeRetained
- testAnnotationDefaultValues
- testAnnotationTargets

---

### CHUNK 2: Model Classes (Depends: CHUNK 1)
**Can run in parallel with CHUNK 3**

**Files**:
- `model/ServerMeta.java`
- `model/ToolMeta.java`
- `model/ResourceMeta.java`
- `model/PromptMeta.java`
- `model/ModelTest.java` (test)

**Spec**: Use Lombok `@Value` for immutable data classes
```java
@Value
public class ServerMeta {
    String name;
    String version;
    String instructions;
    List<ToolMeta> tools;
    List<ResourceMeta> resources;
    List<PromptMeta> prompts;
}

@Value
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
}

@Value
public class ResourceMeta {
    String uri;
    String name;
    String description;
    String mimeType;
    Method method;
    boolean async;
}

@Value
public class PromptMeta {
    String name;
    String description;
    Method method;
    boolean async;
}
```

**Tests**:
- testServerMetaCreation
- testToolMetaImmutability
- testEqualsAndHashCode

---

### CHUNK 3: Annotation Scanner (Depends: CHUNK 1, CHUNK 2)
**Can run in parallel with CHUNK 4**

**Files**:
- `scanner/AnnotationScanner.java`
- `scanner/ValidationException.java`
- `scanner/AnnotationScannerTest.java` (test)

**Spec**:
```java
public class AnnotationScanner {
    public ServerMeta scan(Class<?> clazz) {
        validateServerClass(clazz);
        McpServer ann = clazz.getAnnotation(McpServer.class);

        return new ServerMeta(
            ann.name(),
            ann.version(),
            ann.instructions(),
            scanTools(clazz),
            scanResources(clazz),
            scanPrompts(clazz)
        );
    }

    private List<ToolMeta> scanTools(Class<?> clazz) {
        return stream(clazz.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(McpTool.class))
            .map(this::toToolMeta)
            .toList();
    }

    private void validateServerClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(McpServer.class))
            throw new ValidationException("Missing @McpServer");
        if (!Modifier.isPublic(clazz.getModifiers()))
            throw new ValidationException("Server class must be public");
        // check no-arg constructor exists
    }
}

public class ValidationException extends FastMcpException {
    public ValidationException(String message) { super(message); }
}
```

**Tests**:
- testScanValidServer
- testScanMissingAnnotation_ThrowsException
- testScanPrivateClass_ThrowsException
- testScanTools
- testToolNameDefaultsToMethodName

---

### CHUNK 4: Schema Generator (Depends: CHUNK 2)
**Can run in parallel with CHUNK 3, CHUNK 5**

**Files**:
- `schema/SchemaGenerator.java`
- `schema/SchemaCache.java`
- `schema/SchemaGeneratorTest.java` (test)

**Spec**: Generate JSON Schema from Java `Method` parameters

**Type Mapping**:
```
int/Integer          → {"type": "integer"}
long/Long            → {"type": "integer", "format": "int64"}
double/Double        → {"type": "number"}
boolean/Boolean      → {"type": "boolean"}
String               → {"type": "string"}
Enum                 → {"type": "string", "enum": ["A", "B"]}
POJO                 → {"type": "object", "properties": {...}}
List<T>              → {"type": "array", "items": {...}}
Map<String, T>       → {"type": "object", "additionalProperties": {...}}
```

**Code**:
```java
@Slf4j
public class SchemaGenerator {
    private final SchemaCache cache = new SchemaCache();

    public Map<String, Object> generate(Method method) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new HashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter p : method.getParameters()) {
            String name = getParamName(p);
            props.put(name, typeToSchema(p.getParameterizedType()));
            required.add(name);
        }

        schema.put("properties", props);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> typeToSchema(Type type) {
        if (cache.has(type)) return cache.get(type);

        if (type == int.class || type == Integer.class)
            return Map.of("type", "integer");
        if (type == String.class)
            return Map.of("type", "string");
        // ... etc for all primitive types

        if (type instanceof Class<?> clazz && clazz.isEnum())
            return enumSchema(clazz);

        if (type instanceof ParameterizedType pt)
            return genericSchema(pt);

        if (type instanceof Class<?> clazz && !clazz.isPrimitive())
            return pojoSchema(clazz);

        throw new FastMcpException("Unsupported type: " + type);
    }

    private Map<String, Object> pojoSchema(Class<?> clazz) {
        // Jackson introspection for fields
    }

    private String getParamName(Parameter p) {
        // Try @JsonProperty, fall back to parameter name
        JsonProperty ann = p.getAnnotation(JsonProperty.class);
        if (ann != null && !ann.value().isEmpty())
            return ann.value();
        if (p.isNamePresent())
            return p.getName();
        throw new FastMcpException("Cannot determine parameter name. Use @JsonProperty or -parameters");
    }
}

public class SchemaCache {
    private final Map<Type, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public boolean has(Type t) { return cache.containsKey(t); }
    public Map<String, Object> get(Type t) { return cache.get(t); }
    public void put(Type t, Map<String, Object> schema) { cache.put(t, schema); }
}
```

**Tests**:
- testPrimitiveTypes
- testStringType
- testEnumType
- testListType
- testMapType
- testPojoType
- testCaching
- testMissingParameterName_ThrowsException

---

### CHUNK 5: Adapters (Depends: CHUNK 2)
**Can run in parallel with CHUNK 3, CHUNK 4**

**Files**:
- `adapter/ToolHandler.java`
- `adapter/ResourceHandler.java`
- `adapter/PromptHandler.java`
- `adapter/ArgumentBinder.java`
- `adapter/ResponseMarshaller.java`
- `adapter/AdapterTest.java` (test)

**Spec**: Wrap annotated methods as MCP handler BiFunction

**ToolHandler**:
```java
@Value
public class ToolHandler {
    Object instance;
    ToolMeta meta;
    ArgumentBinder binder;
    ResponseMarshaller marshaller;

    public BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>>
            asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                Object result = meta.getMethod().invoke(instance, args);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(marshaller::marshal);
                } else {
                    return Mono.just(marshaller.marshal(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private CallToolResult errorResult(Exception e) {
        return CallToolResult.builder()
            .content(List.of(new TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
```

**ArgumentBinder**:
```java
public class ArgumentBinder {
    private final ObjectMapper mapper = new ObjectMapper();

    public Object[] bind(Method method, Map<String, Object> args) {
        Parameter[] params = method.getParameters();
        Object[] bound = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String name = getParamName(params[i]);
            Object raw = args.get(name);
            bound[i] = mapper.convertValue(raw, params[i].getType());
        }

        return bound;
    }

    private String getParamName(Parameter p) {
        // Same logic as SchemaGenerator
    }
}
```

**ResponseMarshaller**:
```java
public class ResponseMarshaller {
    private final ObjectMapper mapper = new ObjectMapper();

    public CallToolResult marshal(Object value) {
        if (value == null) return emptyResult();

        String text = switch (value) {
            case String s -> s;
            case Number n -> n.toString();
            case Boolean b -> b.toString();
            default -> {
                try {
                    yield mapper.writeValueAsString(value);
                } catch (Exception e) {
                    throw new FastMcpException("Failed to marshal response", e);
                }
            }
        };

        return CallToolResult.builder()
            .content(List.of(new TextContent(text)))
            .isError(false)
            .build();
    }

    private CallToolResult emptyResult() {
        return CallToolResult.builder()
            .content(List.of())
            .isError(false)
            .build();
    }
}
```

**Tests**:
- testToolHandlerSyncInvocation
- testToolHandlerAsyncInvocation
- testArgumentBinding
- testResponseMarshalling
- testErrorHandling

---

### CHUNK 6: FastMCP Core (Depends: ALL above)
**Must run last, orchestrates everything**

**Files**:
- `core/FastMCP.java`
- `core/TransportType.java`
- `core/FastMCPTest.java` (test)

**Spec**: Main entry point, fluent builder API

```java
@Slf4j
public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGen = new SchemaGenerator();

    private FastMCP(Class<?> serverClass) {
        this.serverClass = serverClass;
    }

    public static FastMCP server(Class<?> clazz) {
        return new FastMCP(clazz);
    }

    public FastMCP stdio() { this.transport = TransportType.STDIO; return this; }
    public FastMCP sse() { this.transport = TransportType.HTTP_SSE; return this; }
    public FastMCP streamable() { this.transport = TransportType.HTTP_STREAMABLE; return this; }

    public McpAsyncServer build() {
        instantiateServer();
        ServerMeta meta = scanner.scan(serverClass);

        McpServerTransportProvider provider = createTransport();
        McpServer.AsyncSpecification spec = McpServer.async(provider)
            .serverInfo(meta.getName(), meta.getVersion());

        if (!meta.getInstructions().isEmpty()) {
            spec.instructions(meta.getInstructions());
        }

        for (ToolMeta tool : meta.getTools()) {
            registerTool(spec, tool);
        }

        for (ResourceMeta res : meta.getResources()) {
            registerResource(spec, res);
        }

        for (PromptMeta prompt : meta.getPrompts()) {
            registerPrompt(spec, prompt);
        }

        return spec.build();
    }

    private void registerTool(McpServer.AsyncSpecification spec, ToolMeta toolMeta) {
        Map<String, Object> schema = schemaGen.generate(toolMeta.getMethod());

        Tool tool = Tool.builder()
            .name(toolMeta.getName())
            .description(toolMeta.getDescription())
            .inputSchema(new JsonSchemaImpl(schema))
            .build();

        ToolHandler handler = new ToolHandler(
            serverInstance,
            toolMeta,
            new ArgumentBinder(),
            new ResponseMarshaller()
        );

        spec.tool(tool, handler.asHandler());
    }

    private McpServerTransportProvider createTransport() {
        ObjectMapper mapper = new ObjectMapper();
        return switch (transport) {
            case STDIO -> new StdioServerTransportProvider(mapper);
            case HTTP_SSE -> new HttpServletSseServerTransportProvider(mapper);
            case HTTP_STREAMABLE -> new HttpServletStreamableServerTransportProvider(mapper);
        };
    }

    private void instantiateServer() {
        try {
            serverInstance = serverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new FastMcpException("Failed to instantiate server", e);
        }
    }

    public void run() {
        McpAsyncServer server = build();
        log.info("FastMCP server started");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down");
            server.close();
        }));
        server.awaitTermination();
    }
}

public enum TransportType {
    STDIO, HTTP_SSE, HTTP_STREAMABLE
}
```

**Tests**:
- testBuildServerFromClass
- testStdioTransport
- testSseTransport
- testToolRegistration
- testEndToEndInvocation

---

### CHUNK 7: MCP SDK Integration - stdio Transport (Depends: ALL previous chunks)
**Priority**: Critical - This makes the server actually work with MCP clients

**Status**: Partially done - `FastMCP.java` has skeleton but needs real MCP SDK integration

**Files to modify**:
- `core/FastMCP.java` - Replace dummy RealMcpAsyncServer with proper MCP SDK integration
- `core/McpAsyncServer.java` - May need to extend/wrap official SDK's McpAsyncServer

**Implementation tasks**:
1. **Import official MCP SDK types**: `io.modelcontextprotocol.sdk.*`
2. **Create real transport provider**: Use `StdioServerTransportProvider` from MCP SDK
3. **Register tools with official SDK**: Use `McpServer.async().serverInfo().tool()` builder pattern
4. **Wire up handlers**: Pass `ToolHandler.asHandler()` BiFunction to official SDK
5. **Implement graceful shutdown**: Proper `close()` and `awaitTermination()` handling

**Code structure**:
```java
import io.modelcontextprotocol.sdk.*;
import io.modelcontextprotocol.sdk.server.*;
import com.fasterxml.jackson.databind.ObjectMapper;

private McpAsyncServer build() {
    instantiateServer();
    
    // Scan annotations
    ServerMeta metadata = new AnnotationScanner().scan(serverClass);
    
    // Create transport
    ObjectMapper mapper = new ObjectMapper();
    McpServerTransportProvider transportProvider = switch (transport) {
        case STDIO -> new StdioServerTransportProvider(mapper);
        case HTTP_SSE -> new HttpServletSseServerTransportProvider(mapper);
        case HTTP_STREAMABLE -> new HttpServletStreamableServerTransportProvider(mapper);
    };
    
    // Build official SDK server
    McpServer.AsyncSpecification spec = McpServer.async(transportProvider)
        .serverInfo(metadata.name(), metadata.version());
    
    // Register tools
    for (ToolMeta toolMeta : metadata.getTools()) {
        Map<String, Object> schema = new SchemaGenerator().generate(toolMeta.getMethod());
        JsonSchema inputSchema = new JsonSchemaImpl(schema);
        
        Tool tool = Tool.builder()
            .name(toolMeta.getName())
            .description(toolMeta.getDescription())
            .inputSchema(inputSchema)
            .build();
        
        ToolHandler handler = new ToolHandler();
        handler.instance = serverInstance;
        handler.meta = toolMeta;
        handler.binder = new ArgumentBinder();
        handler.marshaller = new ResponseMarshaller();
        
        spec.tool(tool, handler.asHandler());
    }
    
    // Register resources (TODO: after ResourceHandler is implemented)
    // Register prompts (TODO: after PromptHandler is implemented)
    
    return spec.build();
}
```

**Tests**:
- testStdioServerStarts
- testToolInvokedViaStdio
- testGracefulShutdown

---

### CHUNK 8: SSE and HTTP_STREAMABLE Transports (Depends: CHUNK 7)
**Priority**: High - Enables web-based MCP clients

**Files to modify**:
- `core/FastMCP.java` - Ensure HTTP_SSE and HTTP_STREAMABLE cases work
- No new files needed - just ensure proper MCP SDK integration

**Implementation tasks**:
1. **Verify HttpServletSseServerTransportProvider** works with FastMCP
2. **Verify HttpServletStreamableServerTransportProvider** works with FastMCP
3. **Add configuration support** for host/port (optional, may rely on SDK defaults)
4. **Test transport switching**: Verify `.sse()`, `.streamable()`, `.stdio()` all work

**Code structure**:
- Mostly verification that CHUNK 7's transport switch statement works correctly
- May need to add port/host configuration methods:
  ```java
  public FastMCP port(int port) {
      this.port = port;
      return this;
  }
  ```

**Tests**:
- testSseTransportBuilder
- testStreamableTransportBuilder
- testMultipleTransports

---

### CHUNK 9: ResourceHandler and PromptHandler (Depends: CHUNK 7)
**Priority**: Medium - Complete the adapter layer

**Files to create**:
- `adapter/ResourceHandler.java`
- `adapter/PromptHandler.java`

**ResourceHandler spec**:
```java
public class ResourceHandler {
    Object instance;
    ResourceMeta meta;
    ArgumentBinder binder;
    ResponseMarshaller marshaller;
    
    public BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bindResource(meta.getMethod(), request.uri());
                Object result = meta.getMethod().invoke(instance, args);
                
                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(marshaller::marshalResource);
                } else {
                    return Mono.just(marshaller.marshalResource(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }
}
```

**PromptHandler spec**:
```java
public class PromptHandler {
    Object instance;
    PromptMeta meta;
    ArgumentBinder binder;
    
    public BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bindPrompt(meta.getMethod(), request.arguments());
                Object result = meta.getMethod().invoke(instance, args);
                
                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(this::toPromptResult);
                } else {
                    return Mono.just(toPromptResult(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }
}
```

**Integration with FastMCP**:
- Add resource registration loop after tool registration in `FastMCP.build()`
- Add prompt registration loop after resource registration

**Tests**:
- testResourceHandlerSyncInvocation
- testResourceHandlerAsyncInvocation
- testPromptHandlerSyncInvocation
- testPromptHandlerAsyncInvocation

---

### CHUNK 10: Create ResourceHandler and PromptHandler (Depends: CHUNK 2, CHUNK 5)
**Priority**: Medium - Complete the adapter layer

**Files to create**:
- `adapter/ResourceHandler.java`
- `adapter/PromptHandler.java`
- `adapter/ResourceHandlerTest.java` (test)
- `adapter/PromptHandlerTest.java` (test)

**ResourceHandler spec**:

```java
package io.github.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.adapter.ResponseMarshaller;
import com.ultrathink.fastmcp.model.ResourceMeta;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

import io.modelcontextprotocol.sdk.*; // MCP SDK types

public class ResourceHandler {
    Object instance;
    ResourceMeta meta;
    ArgumentBinder binder;
    ResponseMarshaller marshaller;

    public BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object result = meta.getMethod().invoke(instance);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(this::toResourceResult);
                } else {
                    return Mono.just(toResourceResult(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private ReadResourceResult toResourceResult(Object value) {
        CallToolResult textResult = marshaller.marshal(value);
        String content = textResult.getContent().isEmpty() ? "" : textResult.getContent().get(0).getText();

        ResourceContent resourceContent = new ResourceContent(
                meta.getMimeType(),
                content
        );

        return ReadResourceResult.builder()
                .contents(List.of(resourceContent))
                .build();
    }

    private ReadResourceResult errorResult(Exception e) {
        ResourceContent errorContent = new ResourceContent(
                "text/plain",
                "Error: " + e.getMessage()
        );

        return ReadResourceResult.builder()
                .contents(List.of(errorContent))
                .build();
    }
}
```

**PromptHandler spec**:

```java
package io.github.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.model.PromptMeta;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

import io.modelcontextprotocol.sdk.*; // MCP SDK types

public class PromptHandler {
    Object instance;
    PromptMeta meta;
    ArgumentBinder binder;

    public BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object result = meta.getMethod().invoke(instance);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(this::toPromptResult);
                } else {
                    return Mono.just(toPromptResult(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private GetPromptResult toPromptResult(Object value) {
        String text = value instanceof String ? (String) value : value.toString();

        PromptMessage message = new PromptMessage(
                "user",
                text
        );

        return GetPromptResult.builder()
                .messages(List.of(message))
                .build();
    }

    private GetPromptResult errorResult(Exception e) {
        PromptMessage error = new PromptMessage(
                "user",
                "Error: " + e.getMessage()
        );

        return GetPromptResult.builder()
                .messages(List.of(error))
                .build();
    }
}
```

**Tests**:
- testResourceHandlerSyncInvocation
- testResourceHandlerAsyncInvocation
- testPromptHandlerSyncInvocation
- testPromptHandlerAsyncInvocation

---

### CHUNK 11: Real MCP SDK Integration (Depends: ALL previous chunks)
**Priority**: Critical - Replace dummy RealMcpAsyncServer with proper MCP SDK integration

**Status**: Not started - `FastMCP.java` has dummy implementation

**Files to modify**:
- `core/FastMCP.java` - Replace dummy with real MCP SDK integration
- `core/McpAsyncServer.java` - May need to remove if wrapping official SDK

**Implementation tasks**:
1. **Import official MCP SDK types**: `io.modelcontextprotocol.sdk.*`, `io.modelcontextprotocol.sdk.server.*`
2. **Create real transport provider**: Use `StdioServerTransportProvider`, `HttpServletSseServerTransportProvider`, `HttpServletStreamableServerTransportProvider`
3. **Register tools with official SDK**: Use `McpServer.async().serverInfo().tool()` builder pattern
4. **Wire up handlers**: Pass `ToolHandler.asHandler()` BiFunction to official SDK
5. **Implement graceful shutdown**: Proper `close()` and `awaitTermination()` handling
6. **Remove dummy classes**: Delete or remove `RealMcpAsyncServer` if still present

**Code structure**:
```java
import io.modelcontextprotocol.sdk.*;
import io.modelcontextprotocol.sdk.server.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGen = new SchemaGenerator();

    private FastMCP(Class<?> serverClass) {
        this.serverClass = serverClass;
    }

    public static FastMCP server(Class<?> clazz) {
        return new FastMCP(clazz);
    }

    public FastMCP stdio() { this.transport = TransportType.STDIO; return this; }
    public FastMCP sse() { this.transport = TransportType.HTTP_SSE; return this; }
    public FastMCP streamable() { this.transport = TransportType.HTTP_STREAMABLE; return this; }

    public McpAsyncServer build() {
        instantiateServer();
        ServerMeta meta = scanner.scan(serverClass);

        McpServerTransportProvider provider = createTransport();
        McpServer.AsyncSpecification spec = McpServer.async(provider)
            .serverInfo(meta.getName(), meta.getVersion());

        if (!meta.getInstructions().isEmpty()) {
            spec.instructions(meta.getInstructions());
        }

        for (ToolMeta tool : meta.getTools()) {
            registerTool(spec, tool);
        }

        for (ResourceMeta res : meta.getResources()) {
            registerResource(spec, res);
        }

        for (PromptMeta prompt : meta.getPrompts()) {
            registerPrompt(spec, prompt);
        }

        return spec.build();
    }

    private void registerTool(McpServer.AsyncSpecification spec, ToolMeta toolMeta) {
        Map<String, Object> schema = schemaGen.generate(toolMeta.getMethod());

        Tool tool = Tool.builder()
            .name(toolMeta.getName())
            .description(toolMeta.getDescription())
            .inputSchema(new JsonSchemaImpl(schema))
            .build();

        ToolHandler handler = new ToolHandler();
        handler.instance = serverInstance;
        handler.meta = toolMeta;
        handler.binder = new ArgumentBinder();
        handler.marshaller = new ResponseMarshaller();

        spec.tool(tool, handler.asHandler());
    }

    private void registerResource(McpServer.AsyncSpecification spec, ResourceMeta resMeta) {
        Resource resource = Resource.builder()
            .uri(resMeta.getUri())
            .name(resMeta.getName())
            .description(resMeta.getDescription())
            .mimeType(resMeta.getMimeType())
            .build();

        ResourceHandler handler = new ResourceHandler();
        handler.instance = serverInstance;
        handler.meta = resMeta;
        handler.marshaller = new ResponseMarshaller();

        spec.resource(resource, handler.asHandler());
    }

    private void registerPrompt(McpServer.AsyncSpecification spec, PromptMeta promptMeta) {
        Prompt prompt = Prompt.builder()
            .name(promptMeta.getName())
            .description(promptMeta.getDescription())
            .build();

        PromptHandler handler = new PromptHandler();
        handler.instance = serverInstance;
        handler.meta = promptMeta;

        spec.prompt(prompt, handler.asHandler());
    }

    private McpServerTransportProvider createTransport() {
        ObjectMapper mapper = new ObjectMapper();
        return switch (transport) {
            case STDIO -> new StdioServerTransportProvider(mapper);
            case HTTP_SSE -> new HttpServletSseServerTransportProvider(mapper);
            case HTTP_STREAMABLE -> new HttpServletStreamableServerTransportProvider(mapper);
        };
    }

    private void instantiateServer() {
        try {
            serverInstance = serverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new FastMcpException("Failed to instantiate server", e);
        }
    }

    public void run() {
        McpAsyncServer server = build();
        log.info("FastMCP server started");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down");
            server.close();
        }));
        server.awaitTermination();
    }
}
```

**Tests**:
- testStdioServerBuilds
- testToolRegistration
- testResourceRegistration
- testPromptRegistration
- testTransportSwitching

---

## Dependency Graph

```
CHUNK 0: Exception ──────┐
                         │
CHUNK 1: Annotations ────┼────────┐
                         │        │
                         ▼        ▼
CHUNK 2: Model ──────> CHUNK 3: Scanner
  │                      │
  ├──────────────────────┼────> CHUNK 4: SchemaGen
  │                      │        │
  └──────────────────────┼────> CHUNK 5: Adapters (partial)
                         │        │
                         └────────┴────> CHUNK 6: FastMCP (dummy)
                         │        │
                         └────────┴────> CHUNK 10: Resource/Prompt Handlers
                         │        │
                         └────────┴────> CHUNK 11: MCP SDK Integration
```

**Parallelization**:
- Start: CHUNK 0, CHUNK 1 (parallel)
- After CHUNK 1: CHUNK 2 (parallel)
- After CHUNK 2: CHUNK 3, CHUNK 4, CHUNK 5 (all parallel)
- After ALL: CHUNK 6 (sequential)
- After CHUNK 6: CHUNK 10 (sequential)
- After CHUNK 10: CHUNK 11 (sequential - CRITICAL)
---

## Maven Dependencies

<dependencies>
    <!-- MCP SDK -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp</artifactId>
        <version>0.16.0</version>
    </dependency>

    <!-- Jackson -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.18.2</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.36</version>
        <scope>provided</scope>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.16</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.16</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <parameters>true</parameters> <!-- For parameter names -->
            </configuration>
        </plugin>
    </plugins>
</build>
---

## Verification Plan

**End-to-End Test**:
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

**Verification Steps**:
1. `mvn clean package` - compiles without errors
2. `java -jar target/calculator-server.jar` - server starts
3. MCP client connects via stdio
4. Client calls tool `add` with `{"a": 3, "b": 5}`
5. Server responds `{"content": [{"type": "text", "text": "8"}], "isError": false}`
6. Ctrl+C gracefully shuts down

**Success Metrics**:
- User code: 10 lines (vs 30+ with official SDK)
- Zero configuration files
- Works with stdio, SSE, streamable HTTP
- Clear error messages on misconfiguration
