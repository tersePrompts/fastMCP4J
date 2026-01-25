package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demo server showcasing all FastMCP4J features:
 * - Tools with enhanced parameter descriptions (@McpParam)
 * - Resources (@McpResource)
 * - Prompts (@McpPrompt)
 * - Async operations (@McpAsync)
 */
@McpServer(name = "echo", version = "2.0.0", instructions = "Comprehensive demo server with tools, resources, and prompts")
@McpPlanner
@McpMemory
@McpTodo
@McpFileWrite
@McpFileRead
public class EchoServer {

    // ============================================
    // TOOLS - Basic and Enhanced Parameters
    // ============================================

    @McpTool(description = "Echo back the message")
    public String echo(String message) {
        return "Echo: " + message;
    }

    @McpTool(description = "Add two numbers with enhanced parameter descriptions")
    public int add(
        @McpParam(
            description = "First number to add",
            examples = {"10", "42", "-5"},
            constraints = "Must be a valid integer"
        )
        int a,

        @McpParam(
            description = "Second number to add",
            examples = {"5", "13", "100"},
            constraints = "Must be a valid integer"
        )
        int b
    ) {
        return a + b;
    }

    @McpTool(name = "search_files", description = "Search for files with advanced filtering")
    public String searchFiles(
        @McpParam(
            description = "Directory path to search in. Can be absolute or relative to project root.",
            examples = {"/home/user/documents", "./src/main/java", "C:\\Users\\User\\Projects"},
            constraints = "Must be a valid directory path",
            hints = "Use '.' for current directory, '..' for parent directory"
        )
        String directory,

        @McpParam(
            description = "File pattern to match. Supports glob wildcards.",
            examples = {"*.java", "**/*.txt", "test_*.py"},
            constraints = "Must be a valid glob pattern",
            hints = "Use ** for recursive search, * for single-level wildcard"
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
            constraints = "Must be between 1 and 1000",
            defaultValue = "50",
            required = false,
            hints = "Lower values improve performance"
        )
        int limit
    ) {
        return String.format("Searching '%s' for '%s' (recursive=%s, limit=%d)",
            directory, pattern, recursive, limit);
    }

    @McpTool(name = "create_user", description = "Create a new user account with validation")
    public Map<String, Object> createUser(
        @McpParam(
            description = "User's full name",
            examples = {"John Doe", "Jane Smith", "Li Ming"},
            constraints = "2-50 characters, letters, spaces, and hyphens only"
        )
        String name,

        @McpParam(
            description = "User's email address (will be used as username)",
            examples = {"user@example.com", "john.doe@company.io"},
            constraints = "Must be valid email format",
            hints = "This email will receive account notifications"
        )
        String email,

        @McpParam(
            description = "User role in the system",
            examples = {"admin", "user", "moderator"},
            constraints = "Must be one of: admin, user, moderator",
            defaultValue = "user",
            required = false
        )
        String role
    ) {
        return Map.of(
            "id", "user_" + System.currentTimeMillis(),
            "name", name,
            "email", email,
            "role", role,
            "created_at", LocalDateTime.now().toString()
        );
    }

    @McpTool(name = "calculate", description = "Perform arithmetic calculations")
    public double calculate(
        @McpParam(
            description = "First operand",
            examples = {"10", "3.14", "-5.5"},
            constraints = "Must be a valid number"
        )
        double a,

        @McpParam(
            description = "Arithmetic operation to perform",
            examples = {"add", "subtract", "multiply", "divide"},
            constraints = "Must be one of: add, subtract, multiply, divide",
            hints = "Division by zero will return an error"
        )
        String operation,

        @McpParam(
            description = "Second operand",
            examples = {"5", "2.71", "0"},
            constraints = "Must be a valid number (cannot be zero for division)"
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

    // ============================================
    // TOOLS - Async Examples
    // ============================================

    @McpTool(description = "Async tool that simulates a long-running operation")
    @McpAsync
    public Mono<String> asyncTask(
        @McpParam(
            description = "Task name to execute",
            examples = {"backup", "sync", "analyze"}
        )
        String taskName,

        @McpParam(
            description = "Delay in milliseconds",
            examples = {"1000", "5000"},
            defaultValue = "1000",
            required = false
        )
        int delayMs
    ) {
        return Mono.just("Task '" + taskName + "' started at " + LocalDateTime.now())
            .delayElement(java.time.Duration.ofMillis(delayMs))
            .map(msg -> msg + " | Completed at " + LocalDateTime.now());
    }

    // ============================================
    // RESOURCES - Different Content Types
    // ============================================

    @McpResource(
        uri = "server://info",
        name = "Server Information",
        description = "Get current server status and information",
        mimeType = "text/plain"
    )
    public String getServerInfo() {
        return String.format("""
            FastMCP4J Echo Server
            Version: 2.0.0
            Status: Running
            Started: %s
            Capabilities: Tools, Resources, Prompts
            """, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @McpResource(
        uri = "server://config",
        name = "Server Configuration",
        description = "Server configuration as JSON",
        mimeType = "application/json"
    )
    public Map<String, Object> getServerConfig() {
        return Map.of(
            "server", Map.of(
                "name", "echo",
                "version", "2.0.0",
                "port", 3002,
                "transport", "streamable"
            ),
            "features", Map.of(
                "tools", true,
                "resources", true,
                "prompts", true,
                "async", true
            ),
            "limits", Map.of(
                "maxRequestSize", "10MB",
                "timeout", "30s"
            )
        );
    }

    @McpResource(
        uri = "server://stats",
        name = "Server Statistics",
        description = "Current server statistics and metrics",
        mimeType = "application/json"
    )
    public String getServerStats() {
        return String.format("""
            {
              "uptime_seconds": %d,
              "requests_handled": 0,
              "tools_registered": 7,
              "resources_registered": 4,
              "prompts_registered": 4,
              "memory_used_mb": %d
            }
            """,
            System.currentTimeMillis() / 1000,
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
        );
    }

    @McpResource(
        uri = "server://capabilities",
        name = "Server Capabilities",
        description = "Detailed server capabilities listing",
        mimeType = "text/plain"
    )
    @McpAsync
    public Mono<String> getCapabilities() {
        return Mono.just("""
            FASTMCP4J SERVER CAPABILITIES
            =============================

            TOOLS:
            - echo: Simple message echo
            - add: Add two numbers (enhanced params)
            - search_files: File search with patterns (enhanced params)
            - create_user: User creation (enhanced params)
            - calculate: Arithmetic operations (enhanced params)
            - asyncTask: Async long-running task

            RESOURCES:
            - server://info: Server information (text)
            - server://config: Configuration (JSON)
            - server://stats: Statistics (JSON)
            - server://capabilities: This listing (text)

            PROMPTS:
            - code_review: Generate code review template
            - create_task: Create task with context
            - debug_assistant: Debug help conversation
            - api_documentation: API docs generator

            FEATURES:
            - Enhanced parameter descriptions with examples
            - Async operation support
            - Multiple content types (text, JSON)
            - Reactive programming with Mono/Flux
            """);
    }

    // ============================================
    // PROMPTS - Different Scenarios
    // ============================================

    @McpPrompt(
        name = "code_review",
        description = "Generate a comprehensive code review template"
    )
    public String codeReviewPrompt() {
        return """
            Please review the following code with these criteria:

            1. **Correctness**: Does the code work as intended?
            2. **Performance**: Are there any performance concerns?
            3. **Security**: Any security vulnerabilities?
            4. **Maintainability**: Is the code easy to understand and maintain?
            5. **Best Practices**: Does it follow language/framework best practices?

            Provide specific feedback with examples where possible.
            """;
    }

    @McpPrompt(
        name = "create_task",
        description = "Create a task description with context"
    )
    public List<McpSchema.PromptMessage> createTaskPrompt(
        @McpParam(
            description = "Project name",
            examples = {"FastMCP4J", "WebApp", "API Service"}
        )
        String project,

        @McpParam(
            description = "Task type",
            examples = {"feature", "bug", "refactor"},
            defaultValue = "feature",
            required = false
        )
        String type
    ) {
        return List.of(
            new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(String.format("""
                    Create a %s task for project: %s

                    Please provide:
                    1. Clear task title
                    2. Detailed description
                    3. Acceptance criteria
                    4. Estimated effort
                    5. Dependencies (if any)
                    """, type, project))
            )
        );
    }

    @McpPrompt(
        name = "debug_assistant",
        description = "Start a debugging conversation with context"
    )
    public List<McpSchema.PromptMessage> debugAssistantPrompt(
        @McpParam(
            description = "Programming language",
            examples = {"Java", "Python", "JavaScript"}
        )
        String language,

        @McpParam(
            description = "Error message or issue description",
            examples = {"NullPointerException", "Memory leak", "Performance degradation"}
        )
        String issue
    ) {
        return List.of(
            new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(String.format("""
                    I'm experiencing the following issue in %s:

                    Issue: %s

                    Can you help me debug this? Please:
                    1. Identify potential root causes
                    2. Suggest debugging steps
                    3. Recommend solutions
                    4. Provide code examples if applicable
                    """, language, issue))
            )
        );
    }

    @McpPrompt(
        name = "api_documentation",
        description = "Generate API documentation template"
    )
    @McpAsync
    public Mono<McpSchema.PromptMessage> apiDocumentationPrompt(
        @McpParam(
            description = "API endpoint path",
            examples = {"/api/users", "/api/v1/products"},
            hints = "Include the full path with version if applicable"
        )
        String endpoint,

        @McpParam(
            description = "HTTP method",
            examples = {"GET", "POST", "PUT", "DELETE"},
            constraints = "Must be valid HTTP method"
        )
        String method
    ) {
        return Mono.just(new McpSchema.PromptMessage(
            McpSchema.Role.USER,
            new McpSchema.TextContent(String.format("""
                Generate comprehensive API documentation for:

                Endpoint: %s %s

                Include:
                1. **Description**: What this endpoint does
                2. **Request**:
                   - Headers required
                   - Request body schema (if applicable)
                   - Query parameters
                3. **Response**:
                   - Success response (200, 201, etc.)
                   - Error responses (400, 401, 404, 500, etc.)
                   - Response body schema
                4. **Examples**: Request and response examples
                5. **Notes**: Any special considerations
                """, method, endpoint))
        ));
    }

    // ============================================
    // MAIN - Server Startup
    // ============================================

    public static void main(String[] args) {
        System.out.println("Starting FastMCP4J Echo Server v2.0.0");
        System.out.println("Features: Tools (7) | Resources (4) | Prompts (4)");
        System.out.println("Transport: HTTP Streamable on port 3002");
        System.out.println("MCP URI: /mcp");
        System.out.println();

        FastMCP.server(EchoServer.class)
            .streamable()
            // .sse()           // Uncomment for SSE transport
            // .stdio()         // Uncomment for STDIO transport
            .port(3002)
            .mcpUri("/mcp")
            .run();
    }
}
