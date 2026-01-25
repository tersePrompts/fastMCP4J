# CHUNK 3: Annotation Scanner

**Dependencies**: CHUNK 1 (Annotations), CHUNK 2 (Model), CHUNK 0 (Exception)

**Files**:
- `src/main/java/io/github/fastmcp/scanner/AnnotationScanner.java`
- `src/main/java/io/github/fastmcp/scanner/ValidationException.java`
- `src/test/java/io/github/fastmcp/scanner/AnnotationScannerTest.java`

---

## Implementation

### ValidationException.java

```java
package com.ultrathink.fastmcp.scanner;

import exception.com.ultrathink.fastmcp.FastMcpException;

public class ValidationException extends FastMcpException {
    public ValidationException(String message) {
        super(message);
    }
}
```

### AnnotationScanner.java

```java
package com.ultrathink.fastmcp.scanner;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.model.PromptMeta;
import com.ultrathink.fastmcp.model.ResourceMeta;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import com.ultrathink.fastmcp.scanner.ValidationException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

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
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(McpTool.class))
                .map(this::toToolMeta)
                .toList();
    }

    private List<ResourceMeta> scanResources(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(McpResource.class))
                .map(this::toResourceMeta)
                .toList();
    }

    private List<PromptMeta> scanPrompts(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(McpPrompt.class))
                .map(this::toPromptMeta)
                .toList();
    }

    private ToolMeta toToolMeta(Method method) {
        McpTool ann = method.getAnnotation(McpTool.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new ToolMeta(name, ann.description(), method, async);
    }

    private ResourceMeta toResourceMeta(Method method) {
        McpResource ann = method.getAnnotation(McpResource.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new ResourceMeta(ann.uri(), name, ann.description(), ann.mimeType(), method, async);
    }

    private PromptMeta toPromptMeta(Method method) {
        McpPrompt ann = method.getAnnotation(McpPrompt.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new PromptMeta(name, ann.description(), method, async);
    }

    private void validateServerClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(McpServer.class)) {
            throw new ValidationException("Missing @McpServer annotation");
        }

        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new ValidationException("Server class must be public");
        }

        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new ValidationException("Server class must have a no-arg constructor");
        }
    }
}
```

---

## Tests

### AnnotationScannerTest.java

```java
package com.ultrathink.fastmcp.scanner;

import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.scanner.AnnotationScanner;
import com.ultrathink.fastmcp.scanner.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationScannerTest {

    private final AnnotationScanner scanner = new AnnotationScanner();

    @McpServer(name = "TestServer", version = "1.0.0")
    public static class ValidServer {
        @McpTool(description = "Test tool")
        public String testTool(String input) {
            return input;
        }
    }

    @Test
    void testScanValidServer() {
        ServerMeta meta = scanner.scan(ValidServer.class);

        assertEquals("TestServer", meta.getName());
        assertEquals("1.0.0", meta.getVersion());
        assertEquals(1, meta.getTools().size());
    }

    @Test
    void testScanMissingAnnotation_ThrowsException() {
        class InvalidServer {
        }

        assertThrows(ValidationException.class, () -> scanner.scan(InvalidServer.class));
    }

    @Test
    void testToolNameDefaultsToMethodName() {
        @McpServer(name = "Test")
        class TestServer {
            @McpTool(description = "Test")
            public void myMethod() {
            }
        }

        ServerMeta meta = scanner.scan(TestServer.class);
        assertEquals("myMethod", meta.getTools().get(0).getName());
    }
}
```

---

## Verification
- [ ] Scanner compiles without errors
- [ ] All tests pass
- [ ] Validation errors have clear messages
- [ ] Tool names default to method names
