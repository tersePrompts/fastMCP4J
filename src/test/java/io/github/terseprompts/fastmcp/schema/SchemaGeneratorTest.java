package io.github.terseprompts.fastmcp.schema;

import io.github.terseprompts.fastmcp.adapter.schema.SchemaGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaGeneratorTest {
    private final SchemaGenerator generator = new SchemaGenerator();

    public static class PrimitivesServer {
        public int add(int a, int b) { return a + b; }
    }

    public static class Pojo {
        public String name;
        public int age;
    }

    enum Color { RED, BLUE }

    public static class EnumServer {
        public Color color(Color c) { return c; }
    }

    @Test
    void testPrimitiveTypes() throws Exception {
        Method m = PrimitivesServer.class.getMethod("add", int.class, int.class);
        Map<String, Object> schema = generator.generate(m);
        Map<String, Object> expected = Map.of(
            "$schema", "https://json-schema.org/draft/2020-12/schema",
            "type", "object",
            "properties", Map.of(
                "a", Map.of("type", "integer"),
                "b", Map.of("type", "integer")
            ),
            "required", List.of("a", "b")
        );
        assertEquals(expected, schema);
    }

    @Test
    void testPojoType() throws Exception {
        Method m = PojoServer.class.getMethod("consume", Pojo.class);
        Map<String, Object> schema = generator.generate(m);
        // Basic structure check (presence of properties)
        assertTrue(schema.containsKey("type"));
        assertEquals("object", schema.get("type"));
    }

    public static class PojoServer {
        public void consume(Pojo p) {}
    }

    @Test
    void testEnumType() throws Exception {
        Method m = EnumServer.class.getMethod("color", Color.class);
        Map<String, Object> schema = generator.generate(m);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertFalse(props.isEmpty());
        Map.Entry<String, Object> first = props.entrySet().iterator().next();
        Map<String, Object> colorSchema = (Map<String, Object>) first.getValue();
        assertNotNull(colorSchema);
        assertEquals("string", colorSchema.get("type"));
        assertArrayEquals(new String[]{"RED", "BLUE"}, (String[]) colorSchema.get("enum"));
    }
}
