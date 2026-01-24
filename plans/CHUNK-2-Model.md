# CHUNK 2: Model Classes

**Dependencies**: CHUNK 1 (Annotations)

**Files**:
- `src/main/java/io/github/fastmcp/model/ServerMeta.java`
- `src/main/java/io/github/fastmcp/model/ToolMeta.java`
- `src/main/java/io/github/fastmcp/model/ResourceMeta.java`
- `src/main/java/io/github/fastmcp/model/PromptMeta.java`
- `src/test/java/io/github/fastmcp/model/ModelTest.java`

---

## Implementation

**Style**: Use Lombok `@Value` for immutable data classes

### ServerMeta.java
```java
package io.github.fastmcp.model;

import lombok.Value;
import java.util.List;

@Value
public class ServerMeta {
    String name;
    String version;
    String instructions;
    List<ToolMeta> tools;
    List<ResourceMeta> resources;
    List<PromptMeta> prompts;
}
```

### ToolMeta.java
```java
package io.github.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
}
```

### ResourceMeta.java
```java
package io.github.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class ResourceMeta {
    String uri;
    String name;
    String description;
    String mimeType;
    Method method;
    boolean async;
}
```

### PromptMeta.java
```java
package io.github.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class PromptMeta {
    String name;
    String description;
    Method method;
    boolean async;
}
```

---

## Tests

### ModelTest.java
```java
package io.github.fastmcp.model;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testServerMetaCreation() {
        ServerMeta meta = new ServerMeta(
            "test",
            "1.0.0",
            "instructions",
            List.of(),
            List.of(),
            List.of()
        );

        assertEquals("test", meta.getName());
        assertEquals("1.0.0", meta.getVersion());
        assertEquals("instructions", meta.getInstructions());
    }

    @Test
    void testToolMetaImmutability() throws Exception {
        Method method = String.class.getMethod("toString");
        ToolMeta meta = new ToolMeta("test", "desc", method, false);

        assertEquals("test", meta.getName());
        assertEquals("desc", meta.getDescription());
        assertEquals(method, meta.getMethod());
        assertFalse(meta.isAsync());
    }

    @Test
    void testEqualsAndHashCode() throws Exception {
        Method method = String.class.getMethod("toString");
        ToolMeta meta1 = new ToolMeta("test", "desc", method, false);
        ToolMeta meta2 = new ToolMeta("test", "desc", method, false);

        assertEquals(meta1, meta2);
        assertEquals(meta1.hashCode(), meta2.hashCode());
    }
}
```

---

## Verification
- [ ] All model classes compile with Lombok
- [ ] All tests pass
- [ ] Immutability enforced (Lombok @Value)
- [ ] equals() and hashCode() work correctly
