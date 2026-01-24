# CHUNK 1: Annotations

**Dependencies**: None (can start immediately, required by all other chunks)

**Files**:
- `src/main/java/io/github/fastmcp/annotations/McpServer.java`
- `src/main/java/io/github/fastmcp/annotations/McpTool.java`
- `src/main/java/io/github/fastmcp/annotations/McpResource.java`
- `src/main/java/io/github/fastmcp/annotations/McpPrompt.java`
- `src/main/java/io/github/fastmcp/annotations/McpAsync.java`
- `src/test/java/io/github/fastmcp/annotations/AnnotationsTest.java`

---

## Implementation

### McpServer.java
```java
package io.github.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface McpServer {
    String name();
    String version() default "1.0.0";
    String instructions() default "";
}
```

### McpTool.java
```java
package io.github.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpTool {
    String name() default "";
    String description();
}
```

### McpResource.java
```java
package io.github.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpResource {
    String uri();
    String name() default "";
    String description() default "";
    String mimeType() default "text/plain";
}
```

### McpPrompt.java
```java
package io.github.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpPrompt {
    String name() default "";
    String description() default "";
}
```

### McpAsync.java
```java
package io.github.fastmcp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface McpAsync {
}
```

---

## Tests

### AnnotationsTest.java

```java
package io.github.fastmcp.annotations;

import com.ultrathink.fastmcp.annotations.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationsTest {

    @Test
    void testAnnotationsAreRuntimeRetained() {
        assertEquals(RUNTIME, McpServer.class.getAnnotation(Retention.class).value());
        assertEquals(RUNTIME, McpTool.class.getAnnotation(Retention.class).value());
        assertEquals(RUNTIME, McpResource.class.getAnnotation(Retention.class).value());
        assertEquals(RUNTIME, McpPrompt.class.getAnnotation(Retention.class).value());
        assertEquals(RUNTIME, McpAsync.class.getAnnotation(Retention.class).value());
    }

    @Test
    void testAnnotationDefaultValues() {
        @McpServer(name = "test")
        class TestServer {
        }

        McpServer ann = TestServer.class.getAnnotation(McpServer.class);
        assertEquals("1.0.0", ann.version());
        assertEquals("", ann.instructions());
    }

    @Test
    void testAnnotationTargets() throws Exception {
        Target serverTarget = McpServer.class.getAnnotation(Target.class);
        assertArrayEquals(new java.lang.annotation.ElementType[]{TYPE}, serverTarget.value());

        Target toolTarget = McpTool.class.getAnnotation(Target.class);
        assertArrayEquals(new java.lang.annotation.ElementType[]{METHOD}, toolTarget.value());
    }
}
```

---

## Verification
- [ ] All 5 annotations compile
- [ ] All tests pass
- [ ] Annotations have correct retention (RUNTIME)
- [ ] Annotations have correct targets
