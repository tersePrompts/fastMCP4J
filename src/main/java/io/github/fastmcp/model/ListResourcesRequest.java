package io.github.fastmcp.model;

import lombok.Value;
import java.util.Map;

@Value
public class ListResourcesRequest {
    String cursor;
    Map<String, Object> _meta;
    
    public static ListResourcesRequest of(String cursor) {
        return new ListResourcesRequest(cursor, null);
    }
    
    public static ListResourcesRequest of(String cursor, Map<String, Object> meta) {
        return new ListResourcesRequest(cursor, meta);
    }
    
    public String getProgressToken() {
        if (_meta == null) return null;
        Object token = _meta.get("progressToken");
        return token != null ? token.toString() : null;
    }
}
