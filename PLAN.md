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
  └──────────────────────┼────> CHUNK 5: Adapters
                         │        │
                         └────────┴────> CHUNK 6: FastMCP
```

**Parallelization**:
- Start: CHUNK 0, CHUNK 1 (parallel)
- After CHUNK 1: CHUNK 2 (parallel)
- After CHUNK 2: CHUNK 3, CHUNK 4, CHUNK 5 (all parallel)
- After ALL: CHUNK 6 (sequential)

---

## Maven Dependencies

```xml
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
```

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

---

### LEGACY SECTIONS TO REMOVE (ignore everything below this line)

### Phase 3: Schema Generator (Priority: Critical)

**Files to Create:**
- `schema/SchemaGenerator.java`
- `schema/TypeInspector.java`
- `schema/SchemaCache.java`
- `exception/SchemaGenerationException.java`

**Key Responsibilities:**
1. Generate JSON schema from Java `Method` signature
2. Handle primitives (int, String, boolean)
3. Handle POJOs (scan fields, getters)
4. Handle collections (List, Set, Map)
5. Handle generics (List<String>, Map<String, Integer>)
6. Respect Jackson annotations (@JsonProperty, @JsonIgnore)

**Schema Generation Rules:**
```
Java Type                → JSON Schema
────────────────────────────────────────────────────
int, Integer             → {"type": "integer"}
long, Long               → {"type": "integer", "format": "int64"}
double, Double           → {"type": "number"}
boolean, Boolean         → {"type": "boolean"}
String                   → {"type": "string"}
Enum                     → {"type": "string", "enum": [...]}
POJO (class)             → {"type": "object", "properties": {...}}
List<T>                  → {"type": "array", "items": {...}}
Map<String, T>           → {"type": "object", "additionalProperties": {...}}
Optional<T>              → schema of T (not required)
```

**Algorithm:**
```java
public class SchemaGenerator {
    private final ObjectMapper objectMapper;
    private final SchemaCache cache;

    public JsonSchema generateInputSchema(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            String paramName = getParameterName(param);
            Map<String, Object> paramSchema = generateTypeSchema(param.getType());
            properties.put(paramName, paramSchema);

            if (!isOptional(param)) {
                required.add(paramName);
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return new JsonSchemaImpl(schema);
    }

    private Map<String, Object> generateTypeSchema(Type type) {
        // Check cache first
        if (cache.has(type)) {
            return cache.get(type);
        }

        // Generate schema based on type
        if (type instanceof Class<?> clazz) {
            return generateClassSchema(clazz);
        } else if (type instanceof ParameterizedType pt) {
            return generateGenericSchema(pt);
        }
        // ... handle other types
    }

    private Map<String, Object> generateClassSchema(Class<?> clazz) {
        if (clazz == String.class) {
            return Map.of("type", "string");
        } else if (clazz == Integer.class || clazz == int.class) {
            return Map.of("type", "integer");
        } else if (/* is POJO */) {
            return generatePojoSchema(clazz);
        }
        // ... handle other cases
    }

    private Map<String, Object> generatePojoSchema(Class<?> clazz) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        // Introspect fields/getters
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (shouldIncludeField(field)) {
                String fieldName = getJsonPropertyName(field);
                properties.put(fieldName, generateTypeSchema(field.getGenericType()));
            }
        }

        schema.put("properties", properties);
        return schema;
    }
}
```

**Jackson Integration:**
- Use `@JsonProperty` to override field names
- Respect `@JsonIgnore` to exclude fields
- Handle `@JsonFormat` for dates/times
- Support `@JsonSubTypes` for polymorphism (future enhancement)

---

### Phase 4: Tool Handler Adapter (Priority: Critical)

**Files to Create:**
- `adapter/ToolHandlerAdapter.java`
- `adapter/ArgumentBinder.java`
- `adapter/ResponseMarshaller.java`
- `exception/HandlerExecutionException.java`

**Key Responsibilities:**
1. Wrap annotated method as BiFunction<Exchange, Request, Mono<Result>>
2. Extract arguments from `CallToolRequest.arguments()` Map
3. Bind arguments to method parameters (type conversion)
4. Invoke method (via reflection)
5. Marshal return value to `CallToolResult`
6. Handle exceptions and convert to error responses

**Design:**
```java
public class ToolHandlerAdapter {
    private final Object serverInstance;
    private final ToolDefinition toolDef;
    private final ArgumentBinder argumentBinder;
    private final ResponseMarshaller marshaller;

    public BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>>
            createAsyncHandler() {

        return (exchange, request) -> {
            try {
                // 1. Extract and bind arguments
                Object[] args = argumentBinder.bind(
                    toolDef.method(),
                    request.arguments()
                );

                // 2. Invoke method
                Object result = toolDef.method().invoke(serverInstance, args);

                // 3. Handle async vs sync
                if (toolDef.isAsync()) {
                    // Method returns Mono<T>
                    return ((Mono<?>) result)
                        .map(value -> marshaller.marshal(value))
                        .onErrorResume(this::handleError);
                } else {
                    // Method returns T directly
                    return Mono.just(marshaller.marshal(result));
                }

            } catch (Exception e) {
                return Mono.just(handleError(e));
            }
        };
    }

    private Mono<CallToolResult> handleError(Throwable error) {
        return Mono.just(CallToolResult.builder()
            .content(List.of(new TextContent(error.getMessage())))
            .isError(true)
            .build());
    }
}
```

**Argument Binding:**
```java
public class ArgumentBinder {
    private final ObjectMapper objectMapper;

    public Object[] bind(Method method, Map<String, Object> arguments) {
        Parameter[] params = method.getParameters();
        Object[] boundArgs = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String paramName = getParameterName(params[i]);
            Object rawValue = arguments.get(paramName);

            // Convert to target type
            boundArgs[i] = objectMapper.convertValue(
                rawValue,
                params[i].getType()
            );
        }

        return boundArgs;
    }

    private String getParameterName(Parameter param) {
        // Try @JsonProperty first
        JsonProperty jsonProp = param.getAnnotation(JsonProperty.class);
        if (jsonProp != null && !jsonProp.value().isEmpty()) {
            return jsonProp.value();
        }

        // Fall back to parameter name (requires -parameters compiler flag)
        if (param.isNamePresent()) {
            return param.getName();
        }

        throw new IllegalArgumentException(
            "Cannot determine parameter name. Use @JsonProperty or compile with -parameters"
        );
    }
}
```

**Response Marshalling:**
```java
public class ResponseMarshaller {
    public CallToolResult marshal(Object returnValue) {
        if (returnValue == null) {
            return CallToolResult.builder()
                .content(List.of())
                .isError(false)
                .build();
        }

        // Convert to text representation
        String text = switch (returnValue) {
            case String s -> s;
            case Number n -> n.toString();
            case Boolean b -> b.toString();
            default -> objectMapper.writeValueAsString(returnValue);
        };

        return CallToolResult.builder()
            .content(List.of(new TextContent(text)))
            .isError(false)
            .build();
    }
}
```

---

### Phase 5: FastMCP Builder (Priority: Critical)

**Files to Create:**
- `core/FastMCP.java`
- `core/TransportType.java`

**API Design:**
```java
public class FastMCP {
    // Static factory methods
    public static FastMCP server(Class<?> serverClass) {
        return new FastMCP(serverClass);
    }

    public static FastMCP server(Object serverInstance) {
        return new FastMCP(serverInstance);
    }

    // Builder methods
    public FastMCP transport(TransportType type) {
        this.transport = type;
        return this;
    }

    public FastMCP stdio() {
        return transport(TransportType.STDIO);
    }

    public FastMCP httpSse() {
        return transport(TransportType.HTTP_SSE);
    }

    // Build and run
    public McpAsyncServer buildAsync() {
        // 1. Scan annotations
        ServerMetadata metadata = scanner.scanServer(serverClass);

        // 2. Create transport provider
        McpServerTransportProvider transportProvider = createTransport();

        // 3. Build official SDK server
        McpServer.AsyncSpecification spec = McpServer.async(transportProvider)
            .serverInfo(metadata.name(), metadata.version());

        // 4. Register tools
        for (ToolDefinition toolDef : metadata.tools()) {
            // Generate schema
            JsonSchema inputSchema = schemaGenerator.generateInputSchema(toolDef.method());

            // Create Tool record
            Tool tool = Tool.builder()
                .name(toolDef.name())
                .description(toolDef.description())
                .inputSchema(inputSchema)
                .build();

            // Create handler
            ToolHandlerAdapter adapter = new ToolHandlerAdapter(
                serverInstance,
                toolDef,
                argumentBinder,
                marshaller
            );

            // Register
            spec.tool(tool, adapter.createAsyncHandler());
        }

        // 5. Register resources (similar to tools)
        // 6. Register prompts (similar to tools)

        return spec.build();
    }

    public McpSyncServer buildSync() {
        return new McpSyncServer(buildAsync());
    }

    public void run() {
        run(TransportType.STDIO);
    }

    public void run(TransportType transport) {
        McpAsyncServer server = transport(transport).buildAsync();
        // Block until shutdown
        server.awaitTermination();
    }
}
```

**Transport Type Enum:**
```java
public enum TransportType {
    STDIO,
    HTTP_SSE,
    HTTP_STREAMABLE
}
```

**Usage Example:**
```java
@McpServer(name = "Calculator", version = "1.0.0")
public class CalculatorServer {

    @McpTool(description = "Add two numbers")
    public int add(int a, int b) {
        return a + b;
    }

    @McpResource(uri = "config://version")
    public String getVersion() {
        return "1.0.0";
    }

    public static void main(String[] args) {
        FastMCP.server(CalculatorServer.class)
            .stdio()
            .run();
    }
}
```

---

### Phase 6: Testing & Examples (Priority: High)

**Test Cases:**
1. **Unit Tests**
   - AnnotationScanner validation
   - SchemaGenerator for all type combinations
   - ArgumentBinder type conversions
   - ResponseMarshaller formatting

2. **Integration Tests**
   - Full server lifecycle (build → run → shutdown)
   - Tool invocation end-to-end
   - Resource access
   - Error handling

3. **Example Projects**
   - `examples/simple-calculator` - Basic arithmetic server
   - `examples/file-browser` - Resource templates demo
   - `examples/async-weather` - @McpAsync with external API calls
   - `examples/pojo-schemas` - Complex object schemas

**Testing Strategy:**
```java
@Test
void testSchemaGenerationForPrimitives() {
    Method method = TestServer.class.getMethod("add", int.class, int.class);
    JsonSchema schema = schemaGenerator.generateInputSchema(method);

    Map<String, Object> expected = Map.of(
        "type", "object",
        "properties", Map.of(
            "a", Map.of("type", "integer"),
            "b", Map.of("type", "integer")
        ),
        "required", List.of("a", "b")
    );

    assertEquals(expected, schema.asMap());
}

@Test
void testToolInvocation() throws Exception {
    @McpServer(name = "Test")
    class TestServer {
        @McpTool(description = "Test")
        public String echo(String message) {
            return message;
        }
    }

    FastMCP fastMcp = FastMCP.server(new TestServer());
    McpAsyncServer server = fastMcp.buildAsync();

    // Use MCP test client to invoke tool
    CallToolResult result = testClient.callTool("echo", Map.of("message", "hello"));

    assertEquals("hello", extractText(result));
    assertFalse(result.isError());
}
```

---

## Critical Files to Modify/Create

### New Files (All in `src/main/java`)
1. `io/github/fastmcp/annotations/McpServer.java`
2. `io/github/fastmcp/annotations/McpTool.java`
3. `io/github/fastmcp/annotations/McpResource.java`
4. `io/github/fastmcp/annotations/McpPrompt.java`
5. `io/github/fastmcp/annotations/McpAsync.java`
6. `io/github/fastmcp/core/FastMCP.java`
7. `io/github/fastmcp/core/ServerMetadata.java`
8. `io/github/fastmcp/core/TransportType.java`
9. `io/github/fastmcp/scanner/AnnotationScanner.java`
10. `io/github/fastmcp/scanner/ToolDefinition.java`
11. `io/github/fastmcp/scanner/ResourceDefinition.java`
12. `io/github/fastmcp/scanner/PromptDefinition.java`
13. `io/github/fastmcp/schema/SchemaGenerator.java`
14. `io/github/fastmcp/schema/TypeInspector.java`
15. `io/github/fastmcp/schema/SchemaCache.java`
16. `io/github/fastmcp/adapter/ToolHandlerAdapter.java`
17. `io/github/fastmcp/adapter/ResourceHandlerAdapter.java`
18. `io/github/fastmcp/adapter/PromptHandlerAdapter.java`
19. `io/github/fastmcp/adapter/ArgumentBinder.java`
20. `io/github/fastmcp/adapter/ResponseMarshaller.java`
21. `io/github/fastmcp/exception/FastMcpException.java`
22. `io/github/fastmcp/exception/ValidationException.java`
23. `io/github/fastmcp/exception/SchemaGenerationException.java`
24. `io/github/fastmcp/exception/HandlerExecutionException.java`

### Configuration Files
1. `pom.xml` - Maven dependencies (official SDK, Jackson, SLF4J)
2. `README.md` - Getting started guide
3. `.gitignore` - Maven/IDE files

---

## Verification Plan

### End-to-End Test Scenario

**Goal**: Verify that a user can create a functioning MCP server with minimal code.

**Steps:**
1. Create a simple server class with annotations
2. Compile with `mvn compile`
3. Run the server
4. Connect an MCP client (Claude Desktop or test client)
5. Invoke a tool and verify response
6. Access a resource and verify content
7. Shutdown gracefully

**Success Criteria:**
```java
// 1. User writes this code (10 lines):
@McpServer(name = "Weather", version = "1.0.0")
public class WeatherServer {
    @McpTool(description = "Get current temperature")
    public double getTemp(String city) {
        return 72.5; // Mock
    }

    public static void main(String[] args) {
        FastMCP.server(WeatherServer.class).run();
    }
}

// 2. Compile: mvn clean package

// 3. Run: java -jar target/weather-server.jar

// 4. Client invokes:
//    Tool: getTemp
//    Args: {"city": "San Francisco"}
//
//    Expected response:
//    {
//      "content": [{"type": "text", "text": "72.5"}],
//      "isError": false
//    }
```

**Verification Checklist:**
- [ ] Annotation scanning works
- [ ] Schema generated correctly from `String city` parameter
- [ ] Tool registered with official SDK
- [ ] Handler invokes method with correct argument
- [ ] Response marshalled to text content
- [ ] No exceptions or errors in logs
- [ ] Server shuts down cleanly on Ctrl+C

---

## Dependencies

```xml
<dependencies>
    <!-- MCP Official SDK -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp</artifactId>
        <version>0.16.0</version>
    </dependency>

    <!-- Jackson for schema generation and JSON handling -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.18.2</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.module</groupId>
        <artifactId>jackson-module-jsonSchema</artifactId>
        <version>2.18.2</version>
    </dependency>

    <!-- SLF4J for logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.16</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Trade-offs & Decisions

### 1. Reflection vs Code Generation
**Decision**: Use reflection
**Rationale**: Simpler implementation for MVP, no build-time complexity. Code gen can be added later for performance.

### 2. Java 17+ Requirement
**Decision**: Target Java 17
**Rationale**: Records, pattern matching, modern APIs. Enterprise Java is moving to 17+.

### 3. Jackson for Schema Generation
**Decision**: Use Jackson ObjectMapper + jackson-module-jsonSchema
**Rationale**: Already a transitive dependency of MCP SDK, widely understood, handles complex types.

### 4. Parameter Name Discovery
**Decision**: Require `-parameters` compiler flag OR `@JsonProperty` annotations
**Rationale**: Java erases parameter names by default. This is the least invasive solution.

### 5. Sync vs Async as Default
**Decision**: Support both, async as internal implementation
**Rationale**: Official SDK is async-first (Reactor). We wrap with sync facade for simplicity but expose async for advanced users.

### 6. Exception Handling Strategy
**Decision**: Convert all exceptions to error CallToolResults
**Rationale**: Prevents server crashes, provides user-friendly errors to clients.

---

## Future Enhancements (v0.2+)

1. **GitHub OAuth Provider**
   - Pre-built `@EnableGitHubAuth` annotation
   - Auto token validation

2. **OpenAPI → MCP Generation**
   - CLI: `fastmcp generate --openapi api.yaml --output GeneratedServer.java`
   - Generates annotated server class from OpenAPI spec

3. **CLI Tooling**
   - `fastmcp dev MyServer.java` - Hot reload development server
   - `fastmcp inspect MyServer.class` - Interactive tool tester

4. **Spring Boot Starter**
   - `spring-boot-starter-fastmcp`
   - Auto-detect @McpServer beans
   - Expose via `/mcp` endpoint

5. **Server Composition**
   - `FastMCP.compose(server1, server2).mount("/v1", server1)`

6. **Advanced Schema Features**
   - JSR-303 validation annotations (`@NotNull`, `@Min`, `@Max`)
   - Custom schema annotations (`@Schema(example="...", deprecated=true)`)

---

## Success Metrics

**MVP (v0.1) Success**:
- User can create a working MCP server in < 20 lines of code
- Reduces boilerplate by 80% vs official SDK
- Zero configuration for common cases
- Clear error messages for misconfigurations

**Adoption Indicators**:
- 3+ GitHub stars in first week
- 1+ external contributor within 1 month
- 5+ production deployments within 3 months

---

## Open Questions

1. **Parameter Naming**: Should we enforce `@JsonProperty` or make `-parameters` mandatory?
   - **Recommendation**: Support both, fail with clear error message if neither present

2. **Resource Templates**: How to handle URI template parameters in @McpResource?
   - **Recommendation**: Support method parameters as template vars: `@McpResource(uri="file://{path}")` binds `String path` param

3. **Error Responses**: Should we auto-format exception stack traces or just message?
   - **Recommendation**: Message only by default, add `FastMCP.verboseErrors(true)` for stack traces

4. **Logging**: What level of logging by default?
   - **Recommendation**: INFO for server lifecycle, DEBUG for tool invocations

---

## Implementation Timeline Estimate

*(Note: No time estimates per guidelines, but sequencing is important)*

**Sequential Dependencies**:
1. Annotations → Scanner → Schema Generator → Handler Adapter → FastMCP Builder
2. Cannot parallelize core components due to dependencies
3. Examples/tests can be developed in parallel with Phase 5-6

**Recommended Implementation Order**:
1. Phase 1 (Annotations) - Foundation
2. Phase 2 (Scanner) - Metadata extraction
3. Phase 3 (Schema Generator) - Most complex logic
4. Phase 4 (Handler Adapter) - Integration glue
5. Phase 5 (FastMCP Builder) - API surface
6. Phase 6 (Testing & Examples) - Validation

