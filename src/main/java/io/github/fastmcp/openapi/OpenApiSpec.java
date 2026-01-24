package io.github.fastmcp.openapi;

import lombok.Value;

import java.util.Map;

@Value
public class OpenApiSpec {
    String openapi;
    OpenApiInfo info;
    Map<String, PathItem> paths;
}
