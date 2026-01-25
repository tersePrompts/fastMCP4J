package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demo server showcasing ALL FastMCP4J features:
 *
 * ANNOTATIONS:
 * - @McpServer: Server metadata with icons
 * - @McpTool: Tools with icons
 * - @McpResource: Resources with icons
 * - @McpPrompt: Prompts with icons
 * - @McpAsync: Async operations
 * - @McpContext: Context injection
 * - @McpPreHook/@McpPostHook: Pre/post hooks
 * - @McpMemory: Memory tool
 * - @McpTodo: Todo tool
 * - @McpPlanner: Planner tool
 * - @McpFileRead: File reading tools
 * - @McpFileWrite: File writing tools
 *
 * FEATURES:
 * - Enhanced parameter descriptions (@McpParam)
 * - Icons support (server, tools, resources, prompts)
 * - Context access (session state, notifications)
 * - Pre/post execution hooks
 * - Reactive programming (Mono/Flux)
 * - Multiple transport types (stdio, sse, streamable)
 * - Annotation-based tool enablement
 */
@McpServer(
    name = "echo",
    version = "3.0.0",
    instructions = "Comprehensive FastMCP4J demo server with all features enabled",
    icons = {
        "https://example.com/server-icon.png:image/png:64x64:light",
        "https://example.com/server-icon-dark.png:image/png:64x64:dark"
    }
)
@McpMemory      // Enables: memory tool for persistent storage
@McpTodo        // Enables: todo_* tools for task management
@McpPlanner     // Enables: planner_* tools for planning
@McpFileRead    // Enables: read_lines, grep, file_stats tools
@McpFileWrite   // Enables: write_file, append_file, delete_file, create_directory tools
public class EchoServer {

    // ============================================
    // PRE/POST HOOKS - Logging & Monitoring
    // ============================================

    @McpPreHook(toolName = "*", order = 1)
    public void globalPreHook(Map<String, Object> args) {
        System.out.println("[HOOK] ⚡ Pre-execution: " + args);
    }

    @McpPostHook(toolName = "*", order = 1)
    public void globalPostHook(Map<String, Object> args, Object result) {
        System.out.println("[HOOK] ✅ Post-execution: " + result);
    }

    @McpPreHook(toolName = "echo", order = 2)
    public void validateEcho(Map<String, Object> args) {
        System.out.println("[HOOK] 🔍 Validating echo parameters...");
        if (args.get("message") == null || args.get("message").toString().trim().isEmpty()) {
            System.out.println("[HOOK] ⚠️ Warning: Empty message");
        }
    }

    @McpPostHook(toolName = "calculate", order = 2)
    public void logCalculation(Map<String, Object> args, Object result) {
        System.out.println("[HOOK] 📊 Calculation logged: " + args + " = " + result);
    }

    // ============================================
    // TOOLS - Basic Examples with Icons
    // ============================================

    @McpTool(
        description = "Echo back the message with timestamp",
        icons = {"https://example.com/echo-icon.png:image/png:32x32"}
    )
    public String echo(
        @McpParam(
            description = "Message to echo back",
            examples = {"Hello, World!", "Testing 123"},
            required = true
        )
        String message
    ) {
        return String.format("[%s] Echo: %s",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
            message);
    }

    @McpTool(
        description = "Perform arithmetic calculations",
        icons = {"https://example.com/calc-icon.png:image/png:32x32"}
    )
    public double calculate(
        @McpParam(
            description = "First number",
            examples = {"10", "25.5"},
            required = true
        )
        double a,

        @McpParam(
            description = "Operation to perform",
            examples = {"add", "subtract", "multiply", "divide"},
            constraints = "Must be one of: add, subtract, multiply, divide",
            required = true
        )
        String operation,

        @McpParam(
            description = "Second number",
            examples = {"5", "12.3"},
            required = true
        )
        double b
    ) {
        return switch (operation.toLowerCase()) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> a / b;
            default -> throw new IllegalArgumentException("Invalid operation: " + operation);
        };
    }

    // ============================================
    // TOOLS - Context Access Examples
    // ============================================

    @McpTool(
        description = "Store a value in session state using Context",
        icons = {"https://example.com/store-icon.png:image/png:32x32"}
    )
    public String storeValue(
        @McpParam(description = "Key to store", required = true)
        String key,

        @McpParam(description = "Value to store", required = true)
        String value,

        @McpContext Context ctx
    ) {
        ctx.setState(key, value);
        ctx.info("Stored: " + key + " = " + value);
        return "Stored '" + key + "' in session state";
    }

    @McpTool(
        description = "Retrieve a value from session state using Context",
        icons = {"https://example.com/retrieve-icon.png:image/png:32x32"}
    )
    public String retrieveValue(
        @McpParam(description = "Key to retrieve", required = true)
        String key,

        @McpContext Context ctx
    ) {
        Object value = ctx.getState(key);
        if (value == null) {
            ctx.warning("Key not found: " + key);
            return "Key '" + key + "' not found in session state";
        }
        ctx.debug("Retrieved: " + key + " = " + value);
        return "Value for '" + key + "': " + value;
    }

    @McpTool(
        description = "Process task with progress reporting using Context",
        icons = {"https://example.com/progress-icon.png:image/png:32x32"}
    )
    public String processWithProgress(
        @McpParam(description = "Number of steps", examples = {"10", "100"})
        int steps,

        @McpContext Context ctx
    ) {
        ctx.info("Starting process with " + steps + " steps");

        for (int i = 1; i <= steps; i++) {
            ctx.reportProgress(i, steps, "Processing step " + i + " of " + steps);
            try {
                Thread.sleep(10); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ctx.info("Process completed!");
        return "Completed " + steps + " steps";
    }

    // ============================================
    // TOOLS - Async Examples with Context
    // ============================================

    @McpTool(
        description = "Async task with progress reporting",
        icons = {"https://example.com/async-icon.png:image/png:32x32"}
    )
    @McpAsync
    public Mono<String> asyncTask(
        @McpParam(
            description = "Task name to execute",
            examples = {"backup", "sync", "analyze"}
        )
        String taskName,

        @McpParam(
            description = "Duration in seconds",
            examples = {"5", "10"},
            defaultValue = "3",
            required = false
        )
        int durationSeconds,

        @McpContext Context ctx
    ) {
        return Mono.fromRunnable(() -> {
            ctx.info("🚀 Starting async task: " + taskName);
        })
        .then(Mono.defer(() -> {
            // Simulate work with progress
            for (int i = 1; i <= durationSeconds; i++) {
                ctx.reportProgress(i, durationSeconds, "Processing " + taskName + " (" + i + "/" + durationSeconds + ")");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ctx.info("✅ Completed async task: " + taskName);
            return Mono.just(String.format("Task '%s' completed in %d seconds at %s",
                taskName, durationSeconds, LocalDateTime.now()));
        }));
    }

    // ============================================
    // RESOURCES - Different Content Types with Icons
    // ============================================

    @McpResource(
        uri = "server://info",
        name = "Server Information",
        description = "Get current server status and information",
        mimeType = "text/plain",
        icons = {"https://example.com/info-icon.png:image/png:32x32"}
    )
    public String getServerInfo() {
        return String.format("""
            ╔════════════════════════════════════════════╗
            ║     FastMCP4J Echo Server v3.0.0         ║
            ╚════════════════════════════════════════════╝

            Status: Running
            Started: %s

            ENABLED FEATURES:
            ✓ Memory Tool (@McpMemory)
            ✓ Todo Tool (@McpTodo)
            ✓ Planner Tool (@McpPlanner)
            ✓ FileRead Tool (@McpFileRead)
            ✓ FileWrite Tool (@McpFileWrite)
            ✓ Icons Support
            ✓ Context Access
            ✓ Pre/Post Hooks
            ✓ Async Operations

            CAPABILITIES:
            - Tools with enhanced parameters
            - Resources with multiple formats
            - Prompts with templates
            - Session state management
            - Progress reporting
            - Notification system
            """, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @McpResource(
        uri = "server://config",
        name = "Server Configuration",
        description = "Server configuration as JSON",
        mimeType = "application/json",
        icons = {"https://example.com/config-icon.png:image/png:32x32"}
    )
    public Map<String, Object> getServerConfig() {
        return Map.of(
            "server", Map.of(
                "name", "echo",
                "version", "3.0.0",
                "transport", "streamable"
            ),
            "features", Map.of(
                "memory", true,
                "todo", true,
                "planner", true,
                "fileRead", true,
                "fileWrite", true,
                "icons", true,
                "context", true,
                "hooks", true,
                "async", true
            ),
            "tools", Map.of(
                "count", 8,
                "async", true
            ),
            "resources", Map.of(
                "count", 4,
                "formats", List.of("text/plain", "application/json")
            ),
            "prompts", Map.of(
                "count", 4,
                "async", true
            )
        );
    }

    @McpResource(
        uri = "server://stats",
        name = "Server Statistics",
        description = "Current server statistics and metrics",
        mimeType = "application/json",
        icons = {"https://example.com/stats-icon.png:image/png:32x32"}
    )
    @McpAsync
    public Mono<String> getServerStats(@McpContext Context ctx) {
        return Mono.fromCallable(() -> {
            ctx.debug("Collecting server statistics...");

            return String.format("""
                {
                  "timestamp": "%s",
                  "uptime_seconds": %d,
                  "memory_used_mb": %d,
                  "memory_total_mb": %d,
                  "session_id": "%s",
                  "features_enabled": 9,
                  "tools_registered": 8,
                  "resources_registered": 4,
                  "prompts_registered": 4
                }
                """,
                LocalDateTime.now(),
                System.currentTimeMillis() / 1000,
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
                Runtime.getRuntime().totalMemory() / (1024 * 1024),
                ctx.getSessionId()
            );
        });
    }

    @McpResource(
        uri = "server://capabilities",
        name = "Server Capabilities",
        description = "Detailed server capabilities listing",
        mimeType = "text/plain",
        icons = {"https://example.com/capabilities-icon.png:image/png:32x32"}
    )
    public String getCapabilities() {
        return """
            ╔══════════════════════════════════════════════════╗
            ║        FASTMCP4J SERVER CAPABILITIES v3.0.0      ║
            ╚══════════════════════════════════════════════════╝

            📦 ANNOTATION-ENABLED FEATURES:
            ────────────────────────────────────────────────────
            @McpMemory      → memory tool (store/retrieve)
            @McpTodo        → todo_* tools (task management)
            @McpPlanner     → planner_* tools (planning)
            @McpFileRead    → read_lines, grep, file_stats
            @McpFileWrite   → write_file, append_file, delete_file

            🛠️  CUSTOM TOOLS (8):
            ────────────────────────────────────────────────────
            • echo                - Echo with timestamp
            • calculate           - Arithmetic operations
            • storeValue          - Store in session (Context)
            • retrieveValue       - Retrieve from session (Context)
            • processWithProgress - Progress reporting (Context)
            • asyncTask           - Async with progress (Context)

            📄 RESOURCES (4):
            ────────────────────────────────────────────────────
            • server://info         - Server information (text)
            • server://config       - Configuration (JSON)
            • server://stats        - Statistics (JSON, async)
            • server://capabilities - This listing (text)

            💬 PROMPTS (4):
            ────────────────────────────────────────────────────
            • code_review          - Code review template
            • create_task          - Task creation template
            • debug_assistant      - Debugging conversation
            • api_documentation    - API docs generator (async)

            🎨 ADVANCED FEATURES:
            ────────────────────────────────────────────────────
            ✓ Icons support (server, tools, resources, prompts)
            ✓ Context injection (@McpContext)
            ✓ Session state management
            ✓ Progress reporting
            ✓ Logging & notifications
            ✓ Pre/Post hooks (@McpPreHook, @McpPostHook)
            ✓ Async operations (@McpAsync with Mono/Flux)
            ✓ Enhanced parameters (@McpParam)
            ✓ Multiple transport types (stdio, sse, streamable)
            """;
    }

    // ============================================
    // PROMPTS - Different Scenarios with Icons
    // ============================================

    @McpPrompt(
        name = "code_review",
        description = "Generate a comprehensive code review template",
        icons = {"https://example.com/review-icon.png:image/png:32x32"}
    )
    public String codeReviewPrompt() {
        return """
            📋 CODE REVIEW CHECKLIST
            ═══════════════════════════════════════

            Please review the following code with these criteria:

            1. ✅ **Correctness**: Does the code work as intended?
            2. ⚡ **Performance**: Are there any performance concerns?
            3. 🔒 **Security**: Any security vulnerabilities?
            4. 🧹 **Maintainability**: Is the code easy to understand and maintain?
            5. 📚 **Best Practices**: Does it follow language/framework best practices?
            6. 🧪 **Testing**: Are there adequate tests?

            Provide specific feedback with examples where possible.
            """;
    }

    @McpPrompt(
        name = "create_task",
        description = "Create a task description with context",
        icons = {"https://example.com/task-icon.png:image/png:32x32"}
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
                    📝 CREATE %s TASK
                    ═══════════════════════════════════════
                    Project: %s

                    Please provide:
                    1. 🎯 Clear task title
                    2. 📖 Detailed description
                    3. ✅ Acceptance criteria
                    4. ⏱️  Estimated effort
                    5. 🔗 Dependencies (if any)
                    """, type.toUpperCase(), project))
            )
        );
    }

    @McpPrompt(
        name = "debug_assistant",
        description = "Start a debugging conversation with context",
        icons = {"https://example.com/debug-icon.png:image/png:32x32"}
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
                    🐛 DEBUG ASSISTANT
                    ═══════════════════════════════════════
                    Language: %s
                    Issue: %s

                    Can you help me debug this? Please:
                    1. 🔍 Identify potential root causes
                    2. 🛠️  Suggest debugging steps
                    3. 💡 Recommend solutions
                    4. 📝 Provide code examples if applicable
                    """, language, issue))
            )
        );
    }

    @McpPrompt(
        name = "api_documentation",
        description = "Generate API documentation template",
        icons = {"https://example.com/api-icon.png:image/png:32x32"}
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
        String method,

        @McpContext Context ctx
    ) {
        ctx.info("Generating API documentation for: " + method + " " + endpoint);

        return Mono.just(new McpSchema.PromptMessage(
            McpSchema.Role.USER,
            new McpSchema.TextContent(String.format("""
                📚 API DOCUMENTATION
                ═══════════════════════════════════════
                Endpoint: %s %s

                Generate comprehensive documentation including:

                1. 📖 **Description**: What this endpoint does
                2. 📥 **Request**:
                   - Headers required
                   - Request body schema (if applicable)
                   - Query parameters
                3. 📤 **Response**:
                   - Success response (200, 201, etc.)
                   - Error responses (400, 401, 404, 500, etc.)
                   - Response body schema
                4. 💡 **Examples**: Request and response examples
                5. 📝 **Notes**: Any special considerations
                6. 🔒 **Authentication**: Required auth method
                """, method, endpoint))
        ));
    }

    // ============================================
    // MAIN - Server Startup
    // ============================================

    public static void main(String[] args) {
        System.out.println("""
            ╔════════════════════════════════════════════════════════════╗
            ║     FastMCP4J Echo Server v3.0.0 - Full Feature Demo     ║
            ╚════════════════════════════════════════════════════════════╝

            📦 Features Enabled:
               ✓ Memory Tool      (@McpMemory)
               ✓ Todo Tool        (@McpTodo)
               ✓ Planner Tool     (@McpPlanner)
               ✓ FileRead Tool    (@McpFileRead)
               ✓ FileWrite Tool   (@McpFileWrite)
               ✓ Icons Support    (server, tools, resources, prompts)
               ✓ Context Access   (@McpContext)
               ✓ Pre/Post Hooks   (@McpPreHook, @McpPostHook)
               ✓ Async Support    (@McpAsync)

            🛠️  Custom Tools: 8
            📄 Resources: 4
            💬 Prompts: 4

            🌐 Transport: HTTP Streamable
            🔌 Port: 3002
            📍 MCP URI: /mcp

            """);

        FastMCP.server(EchoServer.class)
            .streamable()
            // .sse()           // Alternative: SSE transport
            // .stdio()         // Alternative: STDIO transport
            .port(3002)
            .mcpUri("/mcp")
            .run();
    }
}
