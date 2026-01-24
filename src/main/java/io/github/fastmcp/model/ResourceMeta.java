package io.github.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class ResourceMeta {
    String uri;
    String name;
    String description;
    String mimeType;
    Method method;
    boolean async;
}