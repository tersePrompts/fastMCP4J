package com.ultrathink.fastmcp.model;

import com.ultrathink.fastmcp.icons.Icon;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
public class PromptMeta {
    String name;
    String description;
    Method method;
    boolean async;
    List<Icon> icons;

    public PromptMeta() {}

    public PromptMeta(String name, String description, Method method, boolean async) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
    }

    public PromptMeta(String name, String description, Method method, boolean async, List<Icon> icons) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.async = async;
        this.icons = icons;
    }
}