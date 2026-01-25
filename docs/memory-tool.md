# Memory Tool Feature

FastMCP4J now includes a Memory Tool that allows AI agents to persist and retrieve information across sessions, similar to [Claude's Memory Tool](https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool).

## Overview

The Memory Tool provides a file-system-like storage interface where AI agents can:
- Store learned information and context
- Recall past decisions and interactions
- Maintain knowledge bases over time
- Enable cross-conversation learning

## Quick Start

### Basic Usage

```java
import com.ultrathink.fastmcp.annotations.*;

@McpServer(name = "MyServer", version = "1.0.0")
@McpMemory  // Enable memory tool
public class MyServer {

    @McpTool(description = "Process a task")
    public String processTask(String task) {
        // AI can use the memory tool to save progress
        return "Processing: " + task;
    }

    public static void main(String[] args) {
        FastMCP.server(MyServer.class)
            .stdio()
            .run();
    }
}
```

### With Custom Memory Store

```java
import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.memory.*;

@McpServer(name = "MyServer")
@McpMemory
public class MyServer {

    @McpTool(description = "Process a task")
    public String processTask(String task) {
        return "Processing: " + task;
    }

    public static void main(String[] args) {
        // Use custom memory store with larger file size limit
        MemoryStore customStore = new InMemoryMemoryStore("/memories", 50_000_000);

        FastMCP.server(MyServer.class)
            .memoryStore(customStore)
            .stdio()
            .run();
    }
}
```

## API

### `@McpMemory` Annotation

Enable the memory tool by adding `@McpMemory` to your server class:

```java
@McpServer(name = "ServerName")
@McpMemory
public class MyServer {
    // ...
}
```

### `FastMCP.memoryStore()`

Configure a custom memory store implementation:

```java
FastMCP.server(MyServer.class)
    .memoryStore(customStore)
    .stdio()
    .run();
```

## Memory Tool Commands

The memory tool implements the following commands, matching Claude's Memory Tool specification:

### `view`

List directory contents or read file contents:

```json
{
  "command": "view",
  "path": "/memories",
  "view_range": [1, 10]  // Optional: specific line range
}
```

**Directory output:**
```
Here're the files and directories up to 2 levels deep in /memories:
4.0K	/memories
1.5K	/memories/notes.txt
2.0K	/memories/project.xml
```

**File output:**
```
Here's the content of /memories/notes.txt with line numbers:
     1	First line of content
     2	Second line of content
```

### `create`

Create a new file:

```json
{
  "command": "create",
  "path": "/memories/notes.txt",
  "content": "My notes here"
}
```

**Response:** `File created successfully at: /memories/notes.txt`

### `str_replace`

Replace text in a file:

```json
{
  "command": "str_replace",
  "path": "/memories/notes.txt",
  "old_str": "My notes here",
  "new_str": "Updated notes"
}
```

**Response:** Returns the file content with the replacement applied.

**Error cases:**
- File doesn't exist: `Error: The path {path} does not exist.`
- Text not found: `No replacement was performed, old_str did not appear verbatim`
- Multiple occurrences: `No replacement was performed. Multiple occurrences of old_str in lines: {line_numbers}`

### `insert`

Insert text at a specific line:

```json
{
  "command": "insert",
  "path": "/memories/notes.txt",
  "insert_line": 2,
  "insert_text": "New line here\n"
}
```

**Response:** `The file {path} has been edited.`

### `delete`

Delete a file or directory:

```json
{
  "command": "delete",
  "path": "/memories/old_file.txt"
}
```

**Response:** `Successfully deleted {path}`

### `rename`

Rename or move a file/directory:

```json
{
  "command": "rename",
  "old_path": "/memories/draft.txt",
  "new_path": "/memories/final.txt"
}
```

**Response:** `Successfully renamed {old_path} to {new_path}`

## MemoryStore Interface

The `MemoryStore` interface defines the storage contract. It provides methods for:

- **Listing:** `list(path)` - Get directory contents
- **Reading:** `read(path, viewRange)` - Get file contents
- **Creating:** `create(path, content)` - Create new file
- **Replacing:** `replace(path, oldStr, newStr)` - Replace text
- **Inserting:** `insert(path, line, text)` - Insert text at line
- **Deleting:** `delete(path)` - Remove file/directory
- **Renaming:** `rename(oldPath, newPath)` - Move/rename

## InMemoryMemoryStore

The default implementation uses thread-safe in-memory storage:

- **Thread-safe:** Uses `ConcurrentHashMap` and `ReadWriteLock`
- **Configurable limits:**
  - Default max file size: 10MB
  - Default max lines: 999,999
- **No external dependencies:** Pure Java implementation

### Constructor Options

```java
// Default: /memories root, 10MB limit
new InMemoryMemoryStore()

// Custom root path
new InMemoryMemoryStore("/custom/memories")

// Custom root and file size limit
new InMemoryMemoryStore("/custom/memories", 50_000_000)
```

## Custom Memory Store Implementation

You can implement your own storage backend by implementing `MemoryStore`:

```java
public class DatabaseMemoryStore implements MemoryStore {

    @Override
    public List<MemoryEntry> list(String path) throws MemoryException {
        // Implement database-backed listing
    }

    @Override
    public void create(String path, String content) throws MemoryException {
        // Implement database-backed creation
    }

    // ... implement all other methods
}
```

Example custom backends:
- **Redis:** Distributed memory store for multi-server deployments
- **Filesystem:** Persistent storage on disk
- **S3:** Cloud-based storage
- **Database:** SQL/NoSQL backed storage

## Security Considerations

### Path Traversal Protection

The default `InMemoryMemoryStore` includes path normalization and validation. Custom implementations MUST:

- Validate all paths start with the root directory
- Resolve paths to canonical form
- Reject `../` and other traversal patterns
- Reject URL-encoded traversal sequences (`%2e%2e%2f`)

### File Size Limits

Prevent memory exhaustion by:
- Setting appropriate file size limits
- Monitoring total memory usage
- Implementing memory expiration policies

### Sensitive Information

The AI will generally refuse to write sensitive information to memory, but you may want to add additional validation:
- Strip PII (Personally Identifiable Information)
- Filter passwords/credentials
- Implement redaction rules

## Example Use Cases

### 1. Project Context Management

```java
@McpServer(name = "ProjectBot")
@McpMemory
public class ProjectServer {

    @McpTool(description = "Remember project details")
    public String rememberProject(
        @McpParam(description = "Project name")
        String name,
        @McpParam(description = "Project details")
        String details
    ) {
        return "Project remembered: " + name;
    }
}
```

The AI can now:
- Store project structure in `/memories/projects/{name}/structure.txt`
- Remember coding conventions in `/memories/projects/{name}/conventions.md`
- Track decisions in `/memories/projects/{name}/decisions.xml`

### 2. Learning from Interactions

```java
@McpServer(name = "LearningBot")
@McpMemory
public class LearningServer {

    @McpTool(description = "Learn from user feedback")
    public String learnFeedback(
        @McpParam(description = "What the AI should remember")
        String lesson
    ) {
        return "I've learned: " + lesson;
    }
}
```

### 3. Multi-step Workflow Continuity

```java
@McpServer(name = "WorkflowBot")
@McpMemory
public class WorkflowServer {

    @McpTool(description = "Continue workflow")
    public String continueWorkflow(String workflowId) {
        return "Continuing workflow: " + workflowId;
    }
}
```

## Testing

See `MemoryTest.java` for comprehensive test coverage including:
- File creation, reading, writing
- Text replacement and insertion
- Directory operations
- Concurrent access
- Error handling

## See Also

- [Claude Memory Tool Documentation](https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool)
- `src/test/java/com/ultrathink/fastmcp/example/MemoryExampleServer.java` - Example server
- `src/test/java/com/ultrathink/fastmcp/memory/MemoryTest.java` - Test suite
