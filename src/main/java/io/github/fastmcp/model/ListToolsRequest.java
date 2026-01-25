package io.github.fastmcp.model;

import lombok.Value;
import java.util.Map;

@Value
public class ListToolsRequest {
    String cursor;
    Map<String, Object> _meta;
    
    public static ListToolsRequest of(String cursor) {
        return new ListToolsRequest(cursor, null);
    }
    
    public static ListToolsRequest of(String cursor, Map<String, Object> meta) {
        return new ListToolsRequest(cursor, meta);
    }
    
    public String getProgressToken() {
        if (_meta == null) return null;
        Object token = _meta.get("progressToken");
        return token != null ? token.toString() : null;
    }
}
