# @McpVisible Annotation

## Purpose

The `@McpVisible` annotation controls the default visibility of tools, resources, and prompts in MCP (Model Context Protocol) discovery. When a component is marked as not visible, it is excluded from the `tools/list`, `resources/list`, and `prompts/list` responses, effectively hiding it from AI clients while keeping the method callable if its name is known.

## Use Cases

### Internal Tools
Hide helper methods used internally by your server that should not be exposed to AI clients:

```java
@McpTool(description = "Internal cleanup utility")
@McpVisible(false)
public void internalCleanup() { ... }
```

### Admin-Only Features
Hide administrative features that should only be accessible through direct calls:

```java
@McpTool(description = "Reset server state")
@McpVisible(false)
public String resetServer() { ... }
```

### Beta Features
Hide features that are still under development:

```java
@McpTool(description = "Experimental feature")
@McpVisible(false)
public String betaFeature(String input) { ... }
```

### Conditional Visibility
Combine with programmatic checks for dynamic visibility control:

```java
@McpTool(description = "Feature flag controlled")
@McpVisible(isFeatureFlagEnabled())
public String newFeature() { ... }
```

## API Design

```java
@Retention(RUNTIME)
@Target(METHOD)
public @interface McpVisible {
    /** Whether this component should be visible in MCP listings. Default: true */
    boolean value() default true;
}
```

- **Target**: `METHOD` - Can be applied to methods annotated with `@McpTool`, `@McpResource`, or `@McpPrompt`
- **Retention**: `RUNTIME` - Annotation is available at runtime for reflection-based scanning
- **Default Value**: `true` - Components are visible by default, maintaining backward compatibility

## Examples

### Tool Visibility

```java
@McpServer(name = "MyServer", version = "1.0.0")
public class MyServer {

    // Visible by default
    @McpTool(description = "Calculate sum")
    public int add(int a, int b) {
        return a + b;
    }

    // Explicitly hidden
    @McpTool(description = "Internal utility")
    @McpVisible(false)
    public void internalMethod() { ... }

    // Explicitly visible (same as default)
    @McpTool(description = "Public API")
    @McpVisible(true)
    public String publicApi() { ... }
}
```

### Resource Visibility

```java
@McpResource(uri = "file:///public-config.json")
public String publicConfig() { ... }

@McpResource(uri = "file:///internal-config.json")
@McpVisible(false)
public String internalConfig() { ... }
```

### Prompt Visibility

```java
@McpPrompt(description = "Help writing code")
public String codingAssistant(String language) { ... }

@McpPrompt(description = "Internal debugging template")
@McpVisible(false)
public String debugTemplate(String issue) { ... }
```

## Behavior

1. **Discovery**: Invisible components are not included in `tools/list`, `resources/list`, or `prompts/list` responses
2. **Callability**: If the client knows the exact name, invisible components remain callable (this is a visibility control, not an access control)
3. **Default Behavior**: Without `@McpVisible`, all components are visible (backward compatible)
4. **Metadata**: The `visible` field is set on `ToolMeta`, `ResourceMeta`, and `PromptMeta` during scanning

## Implementation Notes

- The `AnnotationScanner` reads `@McpVisible` during class scanning
- The `FastMCP` registration methods filter out invisible components before adding them to the MCP server
- Meta models (`ToolMeta`, `ResourceMeta`, `PromptMeta`) include a `visible` field defaulting to `true`
