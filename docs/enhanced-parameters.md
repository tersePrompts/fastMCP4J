# Enhanced Parameter Descriptions for MCP Tools

FastMCP4J now supports enhanced parameter descriptions through the `@McpParam` annotation. This helps LLMs better understand how to use your tools by providing:

## Features

- **Detailed descriptions**: Clear explanations of what each parameter does
- **Examples**: Sample values showing expected format
- **Constraints**: Validation rules and limitations
- **Optional/Required control**: Mark parameters as optional with defaults
- **Hints**: Additional guidance for LLMs

## Example Usage

```java
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
    boolean recursive
) {
    // Implementation here
}
```

## Generated Schema

This generates the following enhanced JSON schema:

```json
{
  "type": "object",
  "properties": {
    "directory": {
      "type": "string",
      "description": "Directory path to search in. Should be absolute path or relative to project root.",
      "examples": ["/home/user/documents", "./src/main/java", "C:\\Users\\User\\Projects"],
      "constraints": "Must be a valid directory path",
      "hints": "Use '.' for current directory, '..' for parent directory"
    },
    "pattern": {
      "type": "string", 
      "description": "File pattern to match. Supports wildcards like *.java, **/*.txt",
      "examples": ["*.java", "**/test_*.py", "*.json"],
      "constraints": "Must be a valid file pattern",
      "hints": "Use ** for recursive search, * for single-level match"
    },
    "recursive": {
      "type": "boolean",
      "description": "Whether to search recursively through subdirectories",
      "examples": ["true", "false"],
      "default": "true",
      "required": false
    }
  },
  "required": ["directory", "pattern"]
}
```

## Benefits for LLMs

1. **Better Understanding**: Detailed descriptions clarify parameter purpose
2. **Proper Formatting**: Examples show expected input format
3. **Error Prevention**: Constraints help avoid invalid inputs
4. **Smart Defaults**: Optional parameters with sensible defaults
5. **Usage Guidance**: Hints provide additional context

This results in more reliable tool usage by AI assistants and better user experience.