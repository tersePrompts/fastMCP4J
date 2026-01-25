# CHUNK 0: Exceptions

**Dependencies**: None (can start immediately)

**Files**:
- `src/main/java/io/github/fastmcp/exception/FastMcpException.java`
- `src/test/java/io/github/fastmcp/exception/FastMcpExceptionTest.java`

---

## Implementation

### FastMcpException.java
```java
package com.ultrathink.fastmcp.exception;

public class FastMcpException extends RuntimeException {
    public FastMcpException(String message) {
        super(message);
    }

    public FastMcpException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## Tests

### FastMcpExceptionTest.java

```java
package com.ultrathink.fastmcp.exception;

import com.ultrathink.fastmcp.exception.FastMcpException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastMcpExceptionTest {

    @Test
    void testConstructorWithMessage() {
        FastMcpException ex = new FastMcpException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new RuntimeException("root cause");
        FastMcpException ex = new FastMcpException("wrapper", cause);
        assertEquals("wrapper", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
```

---

## Verification
- [ ] Compiles without errors
- [ ] Both tests pass
- [ ] Exception can be thrown and caught
