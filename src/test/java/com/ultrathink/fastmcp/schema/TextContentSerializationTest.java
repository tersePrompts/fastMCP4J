package com.ultrathink.fastmcp.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.adapter.ResponseMarshaller;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test TextContent serialization to see what JSON is produced.
 */
class TextContentSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ResponseMarshaller marshaller = new ResponseMarshaller();

    @Test
    void testTextContentJsonSerialization() throws Exception {
        McpSchema.TextContent textContent = new McpSchema.TextContent("Hello, World!");

        String json = mapper.writeValueAsString(textContent);
        System.out.println("TextContent JSON: " + json);

        // Check that it has required fields
        assertNotNull(json);
        assertTrue(json.contains("text"), "JSON should contain 'text' field");
        assertTrue(json.contains("Hello, World!"), "JSON should contain the text value");
    }

    @Test
    void testCallToolResultJsonSerialization() throws Exception {
        McpSchema.CallToolResult result = marshaller.marshal("Test message");

        String json = mapper.writeValueAsString(result);
        System.out.println("CallToolResult JSON: " + json);

        // Check structure
        assertNotNull(json);
        assertTrue(json.contains("content"), "JSON should contain 'content' field");

        // Parse and check content
        var parsed = mapper.readTree(json);
        assertNotNull(parsed.get("content"));
        assertTrue(parsed.get("content").isArray());

        if (parsed.get("content").size() > 0) {
            var contentItem = parsed.get("content").get(0);
            System.out.println("Content item: " + contentItem);

            // Check for text field
            if (contentItem.has("text")) {
                System.out.println("Text field found: " + contentItem.get("text").asText());
            } else {
                System.out.println("WARNING: Text field NOT found in content item!");
                System.out.println("Fields present: " + contentItem.fieldNames());
            }
        }
    }

    @Test
    void testTextContentFields() throws Exception {
        McpSchema.TextContent textContent = new McpSchema.TextContent("Test");

        // Try to access the text field
        String text = textContent.text();
        System.out.println("TextContent.text() = " + text);
        assertEquals("Test", text);

        // Try to get the type if it exists
        try {
            java.lang.reflect.Field typeField = textContent.getClass().getDeclaredField("type");
            typeField.setAccessible(true);
            Object typeValue = typeField.get(textContent);
            System.out.println("TextContent.type field = " + typeValue);
        } catch (NoSuchFieldException e) {
            System.out.println("No 'type' field found - checking for type() method...");
            try {
                java.lang.reflect.Method typeMethod = textContent.getClass().getMethod("type");
                Object typeValue = typeMethod.invoke(textContent);
                System.out.println("TextContent.type() = " + typeValue);
            } catch (NoSuchMethodException e2) {
                System.out.println("No type() method found either");
            }
        }
    }
}
