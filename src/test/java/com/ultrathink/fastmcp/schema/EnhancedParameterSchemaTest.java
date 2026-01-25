package com.ultrathink.fastmcp.schema;

import com.ultrathink.fastmcp.scanner.AnnotationScanner;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import com.ultrathink.fastmcp.test.EnhancedParameterServer;
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
        assertEquals("Directory path to search in. Should be absolute path or relative to project root.", 
                     directoryParam.get("description"));
        assertEquals("Must be a valid directory path", directoryParam.get("constraints"));
        assertEquals("Use '.' for current directory, '..' for parent directory", directoryParam.get("hints"));
        
        @SuppressWarnings("unchecked")
        java.util.List<String> examples = (java.util.List<String>) directoryParam.get("examples");
        assertTrue(examples.contains("/home/user/documents"));
        assertTrue(examples.contains("./src/main/java"));
        assertTrue(examples.contains("C:\\Users\\User\\Projects"));
        
        // Test pattern parameter
        Map<String, Object> patternParam = (Map<String, Object>) properties.get("pattern");
        assertEquals("File pattern to match. Supports wildcards like *.java, **/*.txt", 
                     patternParam.get("description"));
        assertEquals("Must be a valid file pattern", patternParam.get("constraints"));
        assertEquals("Use ** for recursive search, * for single-level match", patternParam.get("hints"));
        
        // Test optional recursive parameter
        Map<String, Object> recursiveParam = (Map<String, Object>) properties.get("recursive");
        assertEquals("true", recursiveParam.get("default"));
        assertEquals(false, recursiveParam.get("required"));
        
        // Test optional limit parameter
        Map<String, Object> limitParam = (Map<String, Object>) properties.get("limit");
        assertEquals("50", limitParam.get("default"));
        assertEquals(false, limitParam.get("required"));
        assertEquals("Must be positive integer between 1 and 1000", limitParam.get("constraints"));
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
        assertEquals("User's email address", emailParam.get("description"));
        assertEquals("Must be valid email format", emailParam.get("constraints"));
        assertEquals("This will be username for login.", emailParam.get("hints"));
        
        Map<String, Object> roleParam = (Map<String, Object>) properties.get("role");
        assertEquals("user", roleParam.get("default"));
        assertEquals(false, roleParam.get("required"));
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
        assertEquals("Arithmetic operation to perform", operationParam.get("description"));
        assertEquals("Use 'add' for addition, 'subtract' for subtraction, etc.", operationParam.get("hints"));
        assertTrue(operationParam.containsKey("examples"));
    }
}