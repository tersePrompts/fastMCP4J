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
        ToolMeta meta = new ToolMeta("test", "desc", method, false, false);

        assertEquals("test", meta.getName());
        assertEquals("desc", meta.getDescription());
        assertEquals(method, meta.getMethod());
        assertFalse(meta.isAsync());
        assertFalse(meta.isProgressEnabled());
    }

    @Test
    void testEqualsAndHashCode() throws Exception {
        Method method = String.class.getMethod("toString");
        ToolMeta meta1 = new ToolMeta("test", "desc", method, false, false);
        ToolMeta meta2 = new ToolMeta("test", "desc", method, false, false);

        assertEquals(meta1, meta2);
        assertEquals(meta1.hashCode(), meta2.hashCode());
    }
}