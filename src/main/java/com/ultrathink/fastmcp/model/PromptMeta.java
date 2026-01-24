package com.ultrathink.fastmcp.model;

import lombok.Value;
import java.lang.reflect.Method;

@Value
public class PromptMeta {
    String name;
    String description;
    Method method;
    boolean async;
}