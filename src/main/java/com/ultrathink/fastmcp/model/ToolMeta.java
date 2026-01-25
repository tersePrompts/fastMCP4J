package com.ultrathink.fastmcp.model;

import lombok.Data;
import java.lang.reflect.Method;

@Data
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
    
    public ToolMeta() {}
    
    public ToolMeta(String name, String description, Method method, boolean async) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
    }
}