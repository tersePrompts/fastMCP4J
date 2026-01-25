# Icons Feature Implementation

## Overview
This implementation adds full support for the MCP Icons specification to fastMCP4J. Icons provide visual identifiers for servers, tools, resources, and prompts to enhance user interfaces and improve discoverability.

## MCP Specification Reference
https://modelcontextprotocol.io/specification/draft/basic/index#icons

## Implementation Details

### 1. Core Classes

#### `Icon.java`
Located at: `src/main/java/com/ultrathink/fastmcp/icons/Icon.java`

Represents an icon as defined by the MCP specification with the following properties:
- `src`: URI pointing to the icon resource (required) - can be HTTPS URL or data URI
- `mimeType`: Optional MIME type (e.g., image/png, image/jpeg, image/svg+xml, image/webp)
- `sizes`: Optional array of size specifications (e.g., ["48x48"], ["any"], ["48x48", "96x96"])
- `theme`: Optional theme preference ("light" or "dark")

#### `IconValidator.java`
Located at: `src/main/java/com/ultrathink/fastmcp/icons/IconValidator.java`

Validates Icon objects according to MCP security requirements:

**Security Validations:**
- Only allows `https` and `data:` URI schemes
- Rejects forbidden schemes: `javascript:`, `file:`, `ftp:`, `ws:`, `wss:`
- Validates data URI format and ensures image media type
- Validates MIME types against allowed list
- Validates size format (either "any" or "WxH" pattern)
- Validates theme values ("light" or "dark")

**Supported MIME Types:**
- **Required** (clients MUST support): `image/png`, `image/jpeg`, `image/jpg`
- **Recommended** (clients SHOULD support): `image/svg+xml`, `image/webp`

### 2. Annotation Updates

All feature annotations now support an `icons` parameter:

#### `@McpServer`
```java
@McpServer(
    name = "My Server",
    icons = {
        "https://example.com/server-icon.png",
        "https://example.com/server-icon-dark.png:image/png:48x48:dark"
    }
)
```

#### `@McpTool`
```java
@McpTool(
    name = "echo",
    description = "Echoes back input",
    icons = {
        "https://example.com/echo-icon.png:image/png:48x48"
    }
)
```

#### `@McpResource`
```java
@McpResource(
    uri = "file:///config.json",
    icons = {
        "https://example.com/config-icon.png:image/png:48x48"
    }
)
```

#### `@McpPrompt`
```java
@McpPrompt(
    name = "help",
    icons = {
        "https://example.com/help-icon.png:image/png:48x48"
    }
)
```

### 3. Icon String Format

Icons can be specified using a compact string format:

**Format:** `src` or `src:mimeType:sizes:theme`

**Parts are split by colon (`:`) with max 4 parts:**
1. `src` (required) - URI to the icon
2. `mimeType` (optional) - MIME type of the icon
3. `sizes` (optional) - Comma-separated list of sizes (e.g., "48x48,96x96")
4. `theme` (optional) - Theme preference ("light" or "dark")

**Examples:**
```java
// Simple - just src
"https://example.com/icon.png"

// Full specification
"https://example.com/icon.png:image/png:48x48:light"

// Multiple sizes
"https://example.com/icon.png:image/png:48x48,96x96,192x192:dark"

// SVG with scalable format
"https://example.com/icon.svg:image/svg+xml:any:light"

// Data URI
"data:image/svg+xml;base64,PHN2Zy...:any"
```

### 4. Model Updates

All meta classes now include an `icons` field:

- `ServerMeta` - Server-level icons
- `ToolMeta` - Tool-level icons
- `ResourceMeta` - Resource-level icons
- `PromptMeta` - Prompt-level icons

### 5. AnnotationScanner Updates

The `AnnotationScanner` class has been updated to:
- Parse icon strings from annotations into `Icon` objects
- Validate icons using `IconValidator` during scanning
- Include icons in all meta objects

## Security Considerations

This implementation follows MCP security requirements:

1. **URI Scheme Validation**: Only HTTPS and data: schemes are allowed
2. **Forbidden Schemes**: javascript:, file:, ftp:, ws:, wss: are rejected
3. **Data URI Validation**: Data URIs must be properly formatted with image media type
4. **MIME Type Validation**: Only allowed image types are permitted
5. **No Credential Leakage**: Icons should be fetched without credentials (client responsibility)

## Usage Example

```java
@McpServer(
    name = "Example Server",
    version = "1.0.0",
    instructions = "A server with icons",
    icons = {
        "https://example.com/icon.png",
        "https://example.com/icon-dark.png:image/png:48x48:dark"
    }
)
public class ExampleServer {

    @McpTool(
        name = "echo",
        description = "Echoes back the input",
        icons = {"https://example.com/echo-icon.png:image/png:48x48"}
    )
    public String echo(String input) {
        return input;
    }

    @McpResource(
        uri = "file:///config.json",
        icons = {
            "data:image/svg+xml;base64,PHN2Zy...:any"
        }
    )
    public String getConfig() {
        return "{}";
    }
}
```

## Test Coverage

### Unit Tests
Located at: `src/test/java/com/ultrathink/fastmcp/icons/IconValidatorTest.java`

Tests cover:
- Valid HTTPS icons
- Valid data URI icons
- Icons with multiple sizes
- SVG icons with "any" size
- Minimal icons (src only)
- Invalid inputs (null, empty src)
- Forbidden URI schemes
- Unsupported MIME types
- Invalid size formats
- Invalid themes
- Invalid data URIs

### Example Server
Located at: `src/test/java/com/ultrathink/fastmcp/example/IconExampleServer.java`

A complete example server demonstrating all icon features with:
- Server-level icons (PNG and SVG)
- Tool-level icons with multiple sizes
- Resource-level icons (including data URIs)
- Prompt-level icons with theme support

## Files Modified/Created

### New Files:
- `src/main/java/com/ultrathink/fastmcp/icons/Icon.java`
- `src/main/java/com/ultrathink/fastmcp/icons/IconValidator.java`
- `src/test/java/com/ultrathink/fastmcp/icons/IconValidatorTest.java`
- `src/test/java/com/ultrathink/fastmcp/example/IconExampleServer.java`
- `docs/icons-implementation.md` (this file)

### Modified Files:
- `src/main/java/com/ultrathink/fastmcp/annotations/McpServer.java`
- `src/main/java/com/ultrathink/fastmcp/annotations/McpTool.java`
- `src/main/java/com/ultrathink/fastmcp/annotations/McpResource.java`
- `src/main/java/com/ultrathink/fastmcp/annotations/McpPrompt.java`
- `src/main/java/com/ultrathink/fastmcp/model/ServerMeta.java`
- `src/main/java/com/ultrathink/fastmcp/model/ToolMeta.java`
- `src/main/java/com/ultrathink/fastmcp/model/ResourceMeta.java`
- `src/main/java/com/ultrathink/fastmcp/model/PromptMeta.java`
- `src/main/java/com/ultrathink/fastmcp/scanner/AnnotationScanner.java`

## Building and Testing

To build the project:
```bash
mvn clean compile
```

To run tests:
```bash
mvn test
```

To run only icon-related tests:
```bash
mvn test -Dtest=IconValidatorTest
```

## Notes

- Icon validation happens during annotation scanning, so invalid icons will cause server startup to fail
- Multiple icons can be provided for each element to support different contexts (themes, sizes)
- The implementation is fully compliant with the MCP Icons specification as of draft version
