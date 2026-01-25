package io.github.fastmcp.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ListToolsResult {
    List<ToolMeta> tools;
    String nextCursor;
    
    public boolean hasMore() {
        return nextCursor != null;
    }
}
