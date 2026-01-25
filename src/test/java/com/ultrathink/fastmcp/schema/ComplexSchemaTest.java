package com.ultrathink.fastmcp.schema;

import com.ultrathink.fastmcp.adapter.schema.SchemaGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test schema generation for complex types that cause validation errors.
 */
class ComplexSchemaTest {

    private final SchemaGenerator generator = new SchemaGenerator();

    // Test class with methods that have complex parameter types
    static class TestServer {
        public List<Map<String, Object>> testListOfMaps(List<Map<String, Object>> initialTasks) {
            return initialTasks;
        }

        public List<String> testListOfStrings(List<String> lines) {
            return lines;
        }

        public String testSingleString(String value) {
            return value;
        }
    }

    @Test
    void testListOfMapsSchema() throws Exception {
        Method m = TestServer.class.getMethod("testListOfMaps", List.class);
        Map<String, Object> schema = generator.generate(m);

        System.out.println("List<Map<String, Object>> schema:");
        System.out.println(schema);

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties, "Properties should not be null");

        Map<String, Object> initialTasksSchema = (Map<String, Object>) properties.get("initialTasks");
        assertNotNull(initialTasksSchema, "initialTasks schema should not be null");

        // Should be an array type
        assertEquals("array", initialTasksSchema.get("type"), "Should be array type");

        // Check items schema
        Map<String, Object> itemsSchema = (Map<String, Object>) initialTasksSchema.get("items");
        assertNotNull(itemsSchema, "Items schema should not be null");

        // Items should be object type
        assertEquals("object", itemsSchema.get("type"), "Items should be object type");

        // Check additionalProperties - should allow any value for Object type
        // The fix should make this either true, {}, or a schema that allows any value
        Object additionalProps = itemsSchema.get("additionalProperties");
        assertNotNull(additionalProps, "additionalProperties should be present");

        System.out.println("additionalProperties: " + additionalProps);
        System.out.println("additionalProperties type: " + additionalProps.getClass());
    }

    @Test
    void testListOfStringsSchema() throws Exception {
        Method m = TestServer.class.getMethod("testListOfStrings", List.class);
        Map<String, Object> schema = generator.generate(m);

        System.out.println("List<String> schema:");
        System.out.println(schema);

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties, "Properties should not be null");

        Map<String, Object> linesSchema = (Map<String, Object>) properties.get("lines");
        assertNotNull(linesSchema, "lines schema should not be null");

        // Should be an array type
        assertEquals("array", linesSchema.get("type"), "Should be array type");

        // Check items schema
        Map<String, Object> itemsSchema = (Map<String, Object>) linesSchema.get("items");
        assertNotNull(itemsSchema, "Items schema should not be null");

        // Items should be string type
        assertEquals("string", itemsSchema.get("type"), "Items should be string type");

        System.out.println("List<String> schema looks correct");
    }

    @Test
    void testStringSchema() throws Exception {
        Method m = TestServer.class.getMethod("testSingleString", String.class);
        Map<String, Object> schema = generator.generate(m);

        System.out.println("String schema:");
        System.out.println(schema);

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties, "Properties should not be null");

        Map<String, Object> valueSchema = (Map<String, Object>) properties.get("value");
        assertNotNull(valueSchema, "value schema should not be null");

        assertEquals("string", valueSchema.get("type"), "Should be string type");
    }
}
