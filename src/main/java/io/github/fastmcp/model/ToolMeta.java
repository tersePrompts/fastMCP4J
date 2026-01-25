package io.github.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
    boolean progressEnabled;
}