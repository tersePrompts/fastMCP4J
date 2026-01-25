package com.ultrathink.fastmcp.model;

import lombok.Data;
import java.lang.reflect.Method;

@Data
public class ResourceMeta {
    String uri;
    String name;
    String description;
    String mimeType;
    Method method;
    boolean async;
    
    public ResourceMeta() {}
    
    public ResourceMeta(String uri, String name, String description, String mimeType, Method method, boolean async) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.method = method;
        this.async = async;
    }
}