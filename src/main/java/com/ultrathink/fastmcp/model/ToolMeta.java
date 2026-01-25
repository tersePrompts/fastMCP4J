package com.ultrathink.fastmcp.model;

import com.ultrathink.fastmcp.icons.Icon;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
    List<Icon> icons;

    public ToolMeta() {}

    public ToolMeta(String name, String description, Method method, boolean async) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
    }

    public ToolMeta(String name, String description, Method method, boolean async, List<Icon> icons) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
        this.icons = icons;
    }
}