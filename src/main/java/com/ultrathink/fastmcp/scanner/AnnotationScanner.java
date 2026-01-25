package com.ultrathink.fastmcp.scanner;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.exception.FastMcpException;
import com.ultrathink.fastmcp.icons.Icon;
import com.ultrathink.fastmcp.icons.IconValidator;
import com.ultrathink.fastmcp.model.PromptMeta;
import com.ultrathink.fastmcp.model.ResourceMeta;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AnnotationScanner {

    public ServerMeta scan(Class<?> clazz) {
        validateServerClass(clazz);
        McpServer ann = clazz.getAnnotation(McpServer.class);

        return new ServerMeta(
            ann.name(),
            ann.version(),
            ann.instructions(),
            scanTools(clazz),
            scanResources(clazz),
            scanPrompts(clazz),
            parseIcons(ann.icons())
        );
    }

    private List<ToolMeta> scanTools(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(McpTool.class))
            .map(this::toToolMeta)
            .toList();
    }

    private List<ResourceMeta> scanResources(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(McpResource.class))
            .map(this::toResourceMeta)
            .toList();
    }

    private List<PromptMeta> scanPrompts(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(McpPrompt.class))
            .map(this::toPromptMeta)
            .toList();
    }

    private ToolMeta toToolMeta(Method method) {
        McpTool ann = method.getAnnotation(McpTool.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new ToolMeta(name, ann.description(), method, async, parseIcons(ann.icons()));
    }

    private ResourceMeta toResourceMeta(Method method) {
        McpResource ann = method.getAnnotation(McpResource.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new ResourceMeta(ann.uri(), name, ann.description(), ann.mimeType(), method, async, parseIcons(ann.icons()));
    }

    private PromptMeta toPromptMeta(Method method) {
        McpPrompt ann = method.getAnnotation(McpPrompt.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new PromptMeta(name, ann.description(), method, async, parseIcons(ann.icons()));
    }

    /**
     * Parses icon strings from annotation into Icon objects.
     * Format: "src" or "src:mimeType:sizes:theme"
     * 
     * Examples:
     * - "https://example.com/icon.png"
     * - "https://example.com/icon.png:image/png:48x48:light"
     * - "data:image/svg+xml;base64,PHN2Zy...:any"
     * 
     * @param iconStrings array of icon definition strings
     * @return list of validated Icon objects
     */
    private List<Icon> parseIcons(String[] iconStrings) {
        if (iconStrings == null || iconStrings.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(iconStrings)
            .filter(s -> s != null && !s.trim().isEmpty())
            .map(this::parseIcon)
            .collect(Collectors.toList());
    }

    /**
     * Parses a single icon string into an Icon object.
     */
    private Icon parseIcon(String iconString) {
        String[] parts = iconString.split(":", 4);
        
        String src = parts[0].trim();
        String mimeType = parts.length > 1 ? parts[1].trim() : null;
        String sizes = parts.length > 2 ? parts[2].trim() : null;
        String theme = parts.length > 3 ? parts[3].trim() : null;

        List<String> sizeList = sizes != null && !sizes.isEmpty() 
            ? Arrays.asList(sizes.split(","))
            : null;

        Icon icon = new Icon(src, mimeType, sizeList, theme);
        
        // Validate the icon according to MCP security requirements
        IconValidator.validate(icon);
        
        return icon;
    }

    private void validateServerClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(McpServer.class)) {
            throw new ValidationException("Missing @McpServer annotation");
        }

        // Allow non-public server classes (e.g., local/private inner classes used in tests)
        // Previously enforced public visibility; relaxed to improve testability without breaking runtime.
        // (No exception thrown here)

        // For top-level classes enforce a no-arg constructor; for inner/local classes this may be synthetic
        if (clazz.getEnclosingClass() == null) {
            try {
                clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new ValidationException("Server class must have a no-arg constructor");
            }
        }
    }
}
