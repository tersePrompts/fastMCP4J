package com.ultrathink.fastmcp.model;

import lombok.Data;
import java.lang.reflect.Method;

@Data
public class PromptMeta {
    String name;
    String description;
    Method method;
    boolean async;
    
    public PromptMeta() {}
    
    public PromptMeta(String name, String description, Method method, boolean async) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
    }
}