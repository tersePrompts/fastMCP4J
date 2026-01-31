package io.github.terseprompts.fastmcp.model;

import io.github.terseprompts.fastmcp.icons.Icon;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
public class ResourceMeta {
    String uri;
    String name;
    String description;
    String mimeType;
    Method method;
    boolean async;
    List<Icon> icons;

    public ResourceMeta() {}

    public ResourceMeta(String uri, String name, String description, String mimeType, Method method, boolean async) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.method = method;
        this.async = async;
    }

    public ResourceMeta(String uri, String name, String description, String mimeType, Method method, boolean async, List<Icon> icons) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.method = method;
        this.async = async;
        this.icons = icons;
    }
}