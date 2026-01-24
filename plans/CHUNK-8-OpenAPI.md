# CHUNK 8: OpenAPI Generation

**Dependencies**: CHUNK 2 (Model), CHUNK 3 (Scanner), CHUNK 4 (SchemaGenerator)

**Files**:
- `src/main/java/io/github/fastmcp/openapi/OpenApiGenerator.java`
- `src/main/java/io/github/fastmcp/openapi/OpenApiSpec.java`
- `src/test/java/io/github/fastmcp/openapi/OpenApiTest.java`

## Implementation

### OpenApiSpec.java
```java
package io.github.fastmcp.openapi;

import lombok.Value;
import java.util.Map;

@Value
public class OpenApiSpec {
    OpenApiInfo info;
    String openapi;
    Map<String, PathItem> paths;
}

@Value
public class OpenApiInfo {
    String title;
    String version;
    String description;
}

@Value
public class PathItem {
    Operation get;
    Operation post;
}

@Value
public class Operation {
    String summary;
    String description;
    Map<String, Schema> parameters;
    Schema requestBody;
    Map<String, String> responses;
}

@Value
public class Schema {
    String type;
    Map<String, Schema> properties;
    String ref;
}
```

### OpenApiGenerator.java
```java
package io.github.fastmcp.openapi;

import io.github.fastmcp.model.ServerMeta;
import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.schema.SchemaGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenApiGenerator {
    private final SchemaGenerator schemaGenerator;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenApiGenerator() {
        this.schemaGenerator = new SchemaGenerator();
    }

    public OpenApiSpec generate(ServerMeta meta) {
        OpenApiInfo info = new OpenApiInfo(
            meta.getName() + " API",
            meta.getVersion(),
            meta.getInstructions()
        );

        return new OpenApiSpec(
            "3.0.0",
            info,
            buildPaths(meta)
        );
    }

    public String toJson(ServerMeta meta) {
        OpenApiSpec spec = generate(meta);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAPI JSON", e);
        }
    }

    private Map<String, PathItem> buildPaths(ServerMeta meta) {
        return meta.getTools().stream()
            .collect(java.util.stream.Collectors.toMap(
                tool -> "/tools/" + tool.getName(),
                this::toPathItem
            ));
    }

    private PathItem toPathItem(ToolMeta tool) {
        Operation operation = new Operation(
            tool.getDescription(),
            tool.getDescription(),
            java.util.Map.of(),
            null,
            java.util.Map.of(
                "200", "Success",
                "400", "Bad Request",
                "500", "Internal Server Error"
            )
        );

        return new PathItem(null, operation);
    }
}
```

### Add to FastMCP.java
```java
// Add import
import io.github.fastmcp.openapi.OpenApiGenerator;

// Add to FastMCP class
public String generateOpenApi() {
    OpenApiGenerator generator = new OpenApiGenerator();
    ServerMeta meta = scanner.scan(serverClass);
    return generator.toJson(meta);
}

public void generateOpenApiFile(String outputPath) {
    String json = generateOpenApi();
    try {
        java.nio.file.Files.writeString(
            java.nio.file.Path.of(outputPath),
            json
        );
    } catch (Exception e) {
        throw new RuntimeException("Failed to write OpenAPI file", e);
    }
}
```

## Tests
```java
package io.github.fastmcp.openapi;

import io.github.fastmcp.annotations.McpServer;
import io.github.fastmcp.annotations.McpTool;
import io.github.fastmcp.core.FastMCP;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@McpServer(name = "TestServer", version = "1.0.0")
class TestServer {
    @McpTool(description = "Add numbers")
    public int add(int a, int b) {
        return a + b;
    }
}

class OpenApiTest {
    @Test
    void testGenerateOpenApi() {
        String json = FastMCP.server(TestServer.class).generateOpenApi();
        assertNotNull(json);
        assertTrue(json.contains("TestServer API"));
        assertTrue(json.contains("/tools/add"));
    }
}
```
