package com.ultrathink.fastmcp.openapi;

import lombok.Data;

@Data
public class PathItem {
    Operation get;
    Operation post;
    
    public PathItem() {}
    
    public PathItem(Operation get, Operation post) {
        this.get = get;
        this.post = post;
    }
}
