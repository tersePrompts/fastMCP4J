package com.ultrathink.fastmcp.annotations;

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
        class TestServer {}

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