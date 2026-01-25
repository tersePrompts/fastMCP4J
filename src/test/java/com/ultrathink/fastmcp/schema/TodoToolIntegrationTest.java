package com.ultrathink.fastmcp.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.mcptools.todo.TodoStore;
import com.ultrathink.fastmcp.mcptools.todo.TodoTool;
import com.ultrathink.fastmcp.mcptools.todo.InMemoryTodoStore;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TodoTool to verify update operation works correctly.
 */
class TodoToolIntegrationTest {

    private TodoTool todoTool;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        TodoStore store = new InMemoryTodoStore();
        todoTool = new TodoTool(store);
        mapper = new ObjectMapper();
    }

    @Test
    void testAddAndUpdateTodo() throws Exception {
        // Add a todo first
        String addResult = todoTool.todo("add", "Test task", "high", null, null, null, null, null, null, null);
        System.out.println("Add result: " + addResult);

        // Extract ID from add result (format: "Added todo (ID: xxx): ...")
        assertTrue(addResult.contains("ID:"));
        String id = addResult.substring(addResult.indexOf("ID: ") + 4, addResult.indexOf(")"));
        System.out.println("Extracted ID: " + id);

        // Now update the todo
        String updateResult = todoTool.todo("update", null, null, null, null, id, "completed", null, null, null);
        System.out.println("Update result: " + updateResult);

        assertNotNull(updateResult);
        assertFalse(updateResult.isEmpty());
    }

    @Test
    void testUpdateWithInvalidId() {
        // Try to update with invalid ID
        Exception exception = assertThrows(Exception.class, () -> {
            todoTool.todo("update", null, null, null, null, "invalid-id", "completed", null, null, null);
        });

        System.out.println("Exception message: " + exception.getMessage());
        assertNotNull(exception.getMessage());
    }

    @Test
    void testSerializeUpdateResult() throws Exception {
        // Add a todo
        String addResult = todoTool.todo("add", "Test task", "medium", null, null, null, null, null, null, null);
        String id = addResult.substring(addResult.indexOf("ID: ") + 4, addResult.indexOf(")"));

        // Update the todo
        String updateResult = todoTool.todo("update", null, null, null, null, id, "completed", null, null, null);
        System.out.println("Update result: " + updateResult);

        // Simulate what ResponseMarshaller does
        McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
            .content(java.util.List.of(new McpSchema.TextContent(updateResult)))
            .isError(false)
            .build();

        // Serialize to JSON
        String json = mapper.writeValueAsString(result);
        System.out.println("Serialized result: " + json);

        // Verify structure
        var parsed = mapper.readTree(json);
        assertTrue(parsed.has("content"));
        assertTrue(parsed.get("content").isArray());
        assertTrue(parsed.get("content").size() > 0);

        var contentItem = parsed.get("content").get(0);
        assertTrue(contentItem.has("type"), "Content item should have 'type' field");
        assertEquals("text", contentItem.get("type").asText(), "Type should be 'text'");
        assertTrue(contentItem.has("text"), "Content item should have 'text' field");
        assertFalse(contentItem.get("text").asText().isEmpty(), "Text should not be empty");
    }

    @Test
    void testErrorResultSerialization() throws Exception {
        // Create an error result
        McpSchema.CallToolResult errorResult = McpSchema.CallToolResult.builder()
            .content(java.util.List.of(new McpSchema.TextContent("Todo not found: invalid-id")))
            .isError(true)
            .build();

        // Serialize to JSON
        String json = mapper.writeValueAsString(errorResult);
        System.out.println("Serialized error result: " + json);

        // Verify structure
        var parsed = mapper.readTree(json);
        assertTrue(parsed.has("content"));
        assertTrue(parsed.get("isError").asBoolean());

        var contentItem = parsed.get("content").get(0);
        assertTrue(contentItem.has("text"));
        assertEquals("Todo not found: invalid-id", contentItem.get("text").asText());
    }
}
