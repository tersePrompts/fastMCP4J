package com.ultrathink.fastmcp.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import com.ultrathink.fastmcp.schema.SchemaGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

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
            .collect(Collectors.toMap(
                tool -> "/tools/" + tool.getName(),
                this::toPathItem
            ));
    }

    private PathItem toPathItem(ToolMeta tool) {
        Operation operation = new Operation(
            tool.getDescription(),
            tool.getDescription(),
            Map.of(),
            null,
            Map.of(
                "200", "Success",
                "400", "Bad Request",
                "500", "Internal Server Error"
            )
        );

        return new PathItem(null, operation);
    }
}
