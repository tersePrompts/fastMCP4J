package com.ultrathink.fastmcp.openapi;

import lombok.Data;

import java.util.Map;

@Data
public class OpenApiSpec {
    String openapi;
    OpenApiInfo info;
    Map<String, PathItem> paths;
    
    public OpenApiSpec() {}
    
    public OpenApiSpec(String openapi, OpenApiInfo info, Map<String, PathItem> paths) {
        this.openapi = openapi;
        this.info = info;
        this.paths = paths;
    }
}
