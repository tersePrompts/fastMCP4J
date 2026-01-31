package io.github.terseprompts.fastmcp.exception;

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