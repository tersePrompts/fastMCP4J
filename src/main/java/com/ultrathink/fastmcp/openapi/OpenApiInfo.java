package com.ultrathink.fastmcp.openapi;

import lombok.Data;

@Data
public class OpenApiInfo {
    String title;
    String version;
    String description;
    
    public OpenApiInfo() {}
    
    public OpenApiInfo(String title, String version, String description) {
        this.title = title;
        this.version = version;
        this.description = description;
    }
}
