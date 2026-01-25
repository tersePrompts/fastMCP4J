package io.github.fastmcp.model;

import lombok.Value;
import java.util.Map;

@Value
public class ListPromptsRequest {
    String cursor;
    Map<String, Object> _meta;
    
    public static ListPromptsRequest of(String cursor) {
        return new ListPromptsRequest(cursor, null);
    }
    
    public static ListPromptsRequest of(String cursor, Map<String, Object> meta) {
        return new ListPromptsRequest(cursor, meta);
    }
    
    public String getProgressToken() {
        if (_meta == null) return null;
        Object token = _meta.get("progressToken");
        return token != null ? token.toString() : null;
    }
}
