package com.ultrathink.fastmcp.model;

import com.ultrathink.fastmcp.icons.Icon;
import lombok.Value;

import java.lang.reflect.Method;
import java.util.List;

@Value
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
    List<Icon> icons;
}