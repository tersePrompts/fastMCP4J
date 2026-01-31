package io.github.terseprompts.fastmcp.schema;

import io.github.terseprompts.fastmcp.adapter.schema.SchemaGenerator;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import io.github.terseprompts.fastmcp.example.ContextExampleServer;
import io.github.terseprompts.fastmcp.model.ServerMeta;
import io.github.terseprompts.fastmcp.model.ToolMeta;
import io.github.terseprompts.fastmcp.test.EnhancedParameterServer;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EnhancedParameterSchemaTest {

    @Test
    void testEnhancedParameterDescriptions() {
        SchemaGenerator generator = new SchemaGenerator();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(EnhancedParameterServer.class);

        ToolMeta searchTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("search_files"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema = generator.generate(searchTool.getMethod());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        
        // Test directory parameter
        Map<String, Object> directoryParam = (Map<String, Object>) properties.get("directory");
        assertNotNull(directoryParam);
        String dirDesc = (String) directoryParam.get("description");
        assertTrue(dirDesc.contains("Directory path to search in"));
        assertTrue(dirDesc.contains("Examples:"));
        assertTrue(dirDesc.contains("/home/user/documents"));
        assertTrue(dirDesc.contains("./src/main/java"));
        assertTrue(dirDesc.contains("Constraints: Must be a valid directory path"));
        assertTrue(dirDesc.contains("Hints: Use '.' for current directory"));

        // Test pattern parameter
        Map<String, Object> patternParam = (Map<String, Object>) properties.get("pattern");
        String patternDesc = (String) patternParam.get("description");
        assertTrue(patternDesc.contains("File pattern to match"));
        assertTrue(patternDesc.contains("Constraints: Must be a valid file pattern"));
        assertTrue(patternDesc.contains("Hints: Use ** for recursive search"));

        // Test optional recursive parameter
        Map<String, Object> recursiveParam = (Map<String, Object>) properties.get("recursive");
        assertEquals("true", recursiveParam.get("default"));

        // Test optional limit parameter
        Map<String, Object> limitParam = (Map<String, Object>) properties.get("limit");
        assertEquals("50", limitParam.get("default"));
    }

    @Test
    void testUserCreationSchema() {
        SchemaGenerator generator = new SchemaGenerator();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(EnhancedParameterServer.class);

        ToolMeta userTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("create_user"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema = generator.generate(userTool.getMethod());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        
        Map<String, Object> emailParam = (Map<String, Object>) properties.get("email");
        String emailDesc = (String) emailParam.get("description");
        assertTrue(emailDesc.contains("User's email address"));
        assertTrue(emailDesc.contains("Constraints: Must be valid email format"));
        assertTrue(emailDesc.contains("Hints: This will be username for login."));

        Map<String, Object> roleParam = (Map<String, Object>) properties.get("role");
        assertEquals("user", roleParam.get("default"));
    }

    @Test
    void testCalculationSchema() {
        SchemaGenerator generator = new SchemaGenerator();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(EnhancedParameterServer.class);

        ToolMeta calcTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("calculate"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema = generator.generate(calcTool.getMethod());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        
        Map<String, Object> operationParam = (Map<String, Object>) properties.get("operation");
        String opDesc = (String) operationParam.get("description");
        assertTrue(opDesc.contains("Arithmetic operation to perform"));
        assertTrue(opDesc.contains("Examples:"));
        assertTrue(opDesc.contains("Hints: Use 'add' for addition"));
    }

    @Test
    void testContextParameterExcludedFromSchema() {
        SchemaGenerator generator = new SchemaGenerator();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(ContextExampleServer.class);

        // Test a method with Context parameter
        ToolMeta processDataTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("processData"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema = generator.generate(processDataTool.getMethod());

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        // Only "input" should be in properties, not "ctx"
        assertEquals(1, properties.size(), "Context parameter should be excluded from schema");
        assertTrue(properties.containsKey("input"), "input parameter should be present");
        assertFalse(properties.containsKey("ctx"), "ctx parameter should not be present");

        // Test a method with only Context parameter
        ToolMeta incrementCounterTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("incrementCounter"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema2 = generator.generate(incrementCounterTool.getMethod());

        @SuppressWarnings("unchecked")
        Map<String, Object> properties2 = (Map<String, Object>) schema2.get("properties");

        // Should be empty - only parameter is Context
        assertTrue(properties2.isEmpty(), "Context-only method should have empty schema");

        // Test a method with multiple parameters including Context
        ToolMeta storeDataTool = meta.getTools().stream()
            .filter(t -> t.getName().equals("storeData"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> schema3 = generator.generate(storeDataTool.getMethod());

        @SuppressWarnings("unchecked")
        Map<String, Object> properties3 = (Map<String, Object>) schema3.get("properties");

        // Only "key" and "value" should be present, not "ctx"
        assertEquals(2, properties3.size(), "Should have exactly 2 client parameters");
        assertTrue(properties3.containsKey("key"), "key parameter should be present");
        assertTrue(properties3.containsKey("value"), "value parameter should be present");
        assertFalse(properties3.containsKey("ctx"), "ctx parameter should not be present");
    }
}