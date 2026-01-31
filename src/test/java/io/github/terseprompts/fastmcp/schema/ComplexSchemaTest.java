package io.github.terseprompts.fastmcp.schema;

import io.github.terseprompts.fastmcp.adapter.schema.SchemaGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test schema generation for Object.class (fixes initialTasks validation error). */
class ComplexSchemaTest {
    private final SchemaGenerator generator = new SchemaGenerator();

    static class TestServer {
        public List<Map<String, Object>> testListOfMaps(List<Map<String, Object>> initialTasks) {
            return initialTasks;
        }
    }

    @Test
    void testObjectClassGeneratesEmptySchema() throws Exception {
        Method m = TestServer.class.getMethod("testListOfMaps", List.class);
        Map<String, Object> schema = generator.generate(m);

        // Verify additionalProperties is empty schema {} (allows any value)
        var props = (Map<String, Object>) schema.get("properties");
        var items = (Map<String, Object>) ((Map<String, Object>) props.get("initialTasks")).get("items");
        var additionalProps = items.get("additionalProperties");

        assertNotNull(additionalProps);
        assertTrue(additionalProps instanceof Map);
        assertEquals(0, ((Map<?, ?>) additionalProps).size(), "Object.class should generate empty schema");
    }
}
