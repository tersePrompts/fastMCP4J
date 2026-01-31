package io.github.terseprompts.fastmcp.mcptools.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terseprompts.fastmcp.json.ObjectMapperFactory;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;

/**
 * Memory tool handler that implements Claude Memory tool specification.
 * <p>
 * Supports the following commands:
 * - view: List directory contents or read file contents
 * - create: Create a new file
 * - str_replace: Replace text in a file
 * - insert: Insert text at a specific line
 * - delete: Delete a file or directory
 * - rename: Rename or move a file/directory
 */
public class MemoryTool {

    private static final ObjectMapper mapper = ObjectMapperFactory.getShared();

    private final MemoryStore store;
    private final String rootPath;

    public MemoryTool(MemoryStore store) {
        this.store = store;
        this.rootPath = store.getRootPath();
    }

    /**
     * Get the tool schema for registering with MCP server.
     */
    public String getToolSchema() {
        return generateMemoryToolSchema();
    }

    /**
     * Handle a memory tool call.
     */
    public Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleToolCall(
        McpAsyncServerExchange exchange,
        Map<String, Object> args
    ) {
        try {
            JsonNode argsNode = mapper.valueToTree(args);

            if (!argsNode.has("command")) {
                return Mono.just(createErrorResult("Missing 'command' parameter"));
            }

            String command = argsNode.get("command").asText();

            if ("view".equals(command)) {
                return handleView(argsNode);
            } else if ("create".equals(command)) {
                return handleCreate(argsNode);
            } else if ("str_replace".equals(command)) {
                return handleReplace(argsNode);
            } else if ("insert".equals(command)) {
                return handleInsert(argsNode);
            } else if ("delete".equals(command)) {
                return handleDelete(argsNode);
            } else if ("rename".equals(command)) {
                return handleRename(argsNode);
            } else {
                return Mono.just(createErrorResult("Unknown command: " + command));
            }
        } catch (Exception e) {
            return Mono.just(createErrorResult("Error processing command: " + e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleView(JsonNode args) {
        try {
            if (!args.has("path")) {
                return Mono.just(createErrorResult("Missing 'path' parameter"));
            }

            String path = args.get("path").asText();
            String relativePath = toRelativePath(path);

            // Parse optional view_range
            Optional<int[]> viewRange = Optional.empty();
            if (args.has("view_range") && args.get("view_range").isArray()) {
                JsonNode rangeNode = args.get("view_range");
                if (rangeNode.size() == 2) {
                    int start = rangeNode.get(0).asInt();
                    int end = rangeNode.get(1).asInt();
                    viewRange = Optional.of(new int[]{start, end});
                }
            }

            // Check if it's a file
            Optional<MemoryStore.MemoryEntry> metadata = store.getMetadata(relativePath);
            if (metadata.isPresent() && !metadata.get().isDirectory()) {
                // It's a file
                MemoryStore.FileContent content = store.read(relativePath, viewRange);
                return Mono.just(createResult(formatFileContent(content)));
            } else {
                // It's a directory
                List<MemoryStore.MemoryEntry> entries = store.list(relativePath);
                return Mono.just(createResult(formatDirectoryListing(relativePath, entries)));
            }
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleCreate(JsonNode args) {
        try {
            if (!args.has("path")) {
                return Mono.just(createErrorResult("Missing 'path' parameter"));
            }
            if (!args.has("content")) {
                return Mono.just(createErrorResult("Missing 'content' parameter"));
            }

            String path = args.get("path").asText();
            String content = args.get("content").asText();
            String relativePath = toRelativePath(path);

            store.create(relativePath, content);
            return Mono.just(createResult("File created successfully at: " + path));
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleReplace(JsonNode args) {
        try {
            if (!args.has("path")) {
                return Mono.just(createErrorResult("Missing 'path' parameter"));
            }
            if (!args.has("old_str")) {
                return Mono.just(createErrorResult("Missing 'old_str' parameter"));
            }
            if (!args.has("new_str")) {
                return Mono.just(createErrorResult("Missing 'new_str' parameter"));
            }

            String path = args.get("path").asText();
            String oldStr = args.get("old_str").asText();
            String newStr = args.get("new_str").asText();
            String relativePath = toRelativePath(path);

            store.replace(relativePath, oldStr, newStr);

            // Return a snippet of the edited file
            MemoryStore.FileContent content = store.read(relativePath, Optional.empty());
            return Mono.just(createResult(
                "The memory file has been edited.\n" + formatFileContent(content)
            ));
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleInsert(JsonNode args) {
        try {
            if (!args.has("path")) {
                return Mono.just(createErrorResult("Missing 'path' parameter"));
            }
            if (!args.has("insert_line")) {
                return Mono.just(createErrorResult("Missing 'insert_line' parameter"));
            }
            if (!args.has("insert_text")) {
                return Mono.just(createErrorResult("Missing 'insert_text' parameter"));
            }

            String path = args.get("path").asText();
            int insertLine = args.get("insert_line").asInt();
            String insertText = args.get("insert_text").asText();
            String relativePath = toRelativePath(path);

            store.insert(relativePath, insertLine, insertText);
            return Mono.just(createResult("The file " + path + " has been edited."));
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleDelete(JsonNode args) {
        try {
            if (!args.has("path")) {
                return Mono.just(createErrorResult("Missing 'path' parameter"));
            }

            String path = args.get("path").asText();
            String relativePath = toRelativePath(path);

            store.delete(relativePath);
            return Mono.just(createResult("Successfully deleted " + path));
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult> handleRename(JsonNode args) {
        try {
            if (!args.has("old_path")) {
                return Mono.just(createErrorResult("Missing 'old_path' parameter"));
            }
            if (!args.has("new_path")) {
                return Mono.just(createErrorResult("Missing 'new_path' parameter"));
            }

            String oldPath = args.get("old_path").asText();
            String newPath = args.get("new_path").asText();
            String relativeOldPath = toRelativePath(oldPath);
            String relativeNewPath = toRelativePath(newPath);

            store.rename(relativeOldPath, relativeNewPath);
            return Mono.just(createResult("Successfully renamed " + oldPath + " to " + newPath));
        } catch (MemoryException e) {
            return Mono.just(createErrorResult(e.getMessage()));
        }
    }

    private String formatDirectoryListing(String path, List<MemoryStore.MemoryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here're the files and directories up to 2 levels deep in ")
          .append(path)
          .append(", excluding hidden items and node_modules:\n");

        for (MemoryStore.MemoryEntry entry : entries) {
            sb.append(formatSize(entry.size()))
              .append("\t")
              .append(path.isEmpty() ? "" : path + "/")
              .append(entry.path())
              .append("\n");
        }

        return sb.toString();
    }

    private String formatFileContent(MemoryStore.FileContent content) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here's the content of ")
          .append(content.path())
          .append(" with line numbers:\n");

        content.lineNumbersToContent().forEach((lineNum, lineContent) -> {
            sb.append(String.format("%6d\t%s\n", lineNum, lineContent));
        });

        return sb.toString();
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fK", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fM", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1fG", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String toRelativePath(String fullPath) {
        if (fullPath.startsWith(rootPath)) {
            String relative = fullPath.substring(rootPath.length());
            return relative.startsWith("/") ? relative.substring(1) : relative;
        }
        return fullPath;
    }

    private io.modelcontextprotocol.spec.McpSchema.CallToolResult createResult(String content) {
        return McpSchema.CallToolResult.builder()
            .content(List.of(
                new McpSchema.TextContent(content)
            ))
            .isError(false)
            .build();
    }

    private io.modelcontextprotocol.spec.McpSchema.CallToolResult createErrorResult(String message) {
        return McpSchema.CallToolResult.builder()
            .content(List.of(
                new McpSchema.TextContent(message)
            ))
            .isError(true)
            .build();
    }

    private String generateMemoryToolSchema() {
        return "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"command\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"view\", \"create\", \"str_replace\", \"insert\", \"delete\", \"rename\"],\n" +
            "      \"description\": \"The command to execute\"\n" +
            "    },\n" +
            "    \"path\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The path to operate on\"\n" +
            "    },\n" +
            "    \"content\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The file content (for create command)\"\n" +
            "    },\n" +
            "    \"old_str\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The text to replace (for str_replace command)\"\n" +
            "    },\n" +
            "    \"new_str\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The replacement text (for str_replace command)\"\n" +
            "    },\n" +
            "    \"insert_line\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"description\": \"The line number to insert at (for insert command)\"\n" +
            "    },\n" +
            "    \"insert_text\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The text to insert (for insert command)\"\n" +
            "    },\n" +
            "    \"view_range\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\"type\": \"integer\"},\n" +
            "      \"minItems\": 2,\n" +
            "      \"maxItems\": 2,\n" +
            "      \"description\": \"[start, end] line range (1-indexed, for view command)\"\n" +
            "    },\n" +
            "    \"old_path\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The old path (for rename command)\"\n" +
            "    },\n" +
            "    \"new_path\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"The new path (for rename command)\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }
}
