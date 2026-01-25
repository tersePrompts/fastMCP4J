package com.ultrathink.fastmcp.test;

import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.annotations.McpParam;

/**
 * Test server class to demonstrate enhanced parameter descriptions.
 */
@McpServer(name = "EnhancedParameterTest", version = "1.0.0")
public class EnhancedParameterServer {

    @McpTool(name = "search_files", description = "Search files with advanced filtering options")
    public String searchFiles(
        @McpParam(
            description = "Directory path to search in. Should be absolute path or relative to project root.",
            examples = {"/home/user/documents", "./src/main/java", "C:\\Users\\User\\Projects"},
            constraints = "Must be a valid directory path",
            hints = "Use '.' for current directory, '..' for parent directory"
        )
        String directory,
        
        @McpParam(
            description = "File pattern to match. Supports wildcards like *.java, **/*.txt",
            examples = {"*.java", "**/test_*.py", "*.json"},
            constraints = "Must be a valid file pattern",
            hints = "Use ** for recursive search, * for single-level match"
        )
        String pattern,
        
        @McpParam(
            description = "Whether to search recursively through subdirectories",
            examples = {"true", "false"},
            defaultValue = "true",
            required = false
        )
        boolean recursive,
        
        @McpParam(
            description = "Maximum number of results to return",
            examples = {"10", "50", "100"},
            constraints = "Must be positive integer between 1 and 1000",
            defaultValue = "50",
            required = false
        )
        int limit
    ) {
        return String.format("Searching in %s for pattern %s (recursive: %s, limit: %d)", 
                           directory, pattern, recursive, limit);
    }

    @McpTool(name = "create_user", description = "Create a new user account")
    public String createUser(
        @McpParam(
            description = "User's full name",
            examples = {"John Doe", "Jane Smith", "Zhang San"},
            constraints = "Must be 2-50 characters, no special characters except hyphen and space"
        )
        String name,
        
        @McpParam(
            description = "User's email address",
            examples = {"user@example.com", "john.doe@company.co.uk"},
            constraints = "Must be valid email format",
            hints = "This will be username for login."
        )
        String email,
        
        @McpParam(
            description = "User's role in the system",
            examples = {"admin", "user", "moderator"},
            constraints = "Must be one of: admin, user, moderator",
            defaultValue = "user",
            required = false
        )
        String role
    ) {
        return String.format("Created user: %s (%s) with role: %s", name, email, role);
    }

    @McpTool(name = "calculate", description = "Perform basic arithmetic calculations")
    public double calculate(
        @McpParam(
            description = "First number for calculation",
            examples = {"10", "3.14", "-5"},
            constraints = "Must be a valid number"
        )
        double a,
        
        @McpParam(
            description = "Arithmetic operation to perform",
            examples = {"add", "subtract", "multiply", "divide"},
            constraints = "Must be one of: add, subtract, multiply, divide",
            hints = "Use 'add' for addition, 'subtract' for subtraction, etc."
        )
        String operation,
        
        @McpParam(
            description = "Second number for calculation",
            examples = {"5", "2.71", "0"},
            constraints = "Must be a valid number. For division, cannot be zero."
        )
        double b
    ) {
        return switch (operation.toLowerCase()) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> {
                if (b == 0) throw new IllegalArgumentException("Cannot divide by zero");
                yield a / b;
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }
}