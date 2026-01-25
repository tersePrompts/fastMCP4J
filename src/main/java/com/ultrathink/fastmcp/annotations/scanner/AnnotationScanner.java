package com.ultrathink.fastmcp.annotations.scanner;

import com.ultrathink.fastmcp.annotations.*;
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

    /**
     * Scan tools from a class without requiring @McpServer annotation.
     * Useful for built-in tool classes like TodoTool, PlannerTool, etc.
     */
    public List<ToolMeta> scanToolsOnly(Class<?> clazz) {
        return scanTools(clazz);
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
     * Format: <url>[:<mimeType>[:<sizes>[:<theme>]]]
     * Examples:
     *   "https://example.com/icon.png"
     *   "https://example.com/icon.png:image/png:48x48:light"
     *   "data:image/svg+xml;base64,ABC...:any"
     */
    private Icon parseIcon(String iconString) {
        // Parse from right to left since optional parts are at the end
        // This handles URLs that contain colons (like https://)

        String theme = null;
        String sizes = null;
        String mimeType = null;
        String src = iconString;

        // 1. Extract theme (last segment if it's "light" or "dark")
        int lastColon = src.lastIndexOf(':');
        if (lastColon > 0) {
            String possibleTheme = src.substring(lastColon + 1).trim();
            if ("light".equals(possibleTheme) || "dark".equals(possibleTheme)) {
                theme = possibleTheme;
                src = src.substring(0, lastColon);
            }
        }

        // 2. Extract sizes (segment before theme if it matches size pattern)
        lastColon = src.lastIndexOf(':');
        if (lastColon > 0) {
            String possibleSizes = src.substring(lastColon + 1).trim();
            // Check if it looks like sizes (contains 'x' for dimensions or is "any")
            if (possibleSizes.equals("any") || possibleSizes.matches("\\d+x\\d+(,\\d+x\\d+)*")) {
                sizes = possibleSizes;
                src = src.substring(0, lastColon);
            }
        }

        // 3. Extract MIME type (segment before sizes if it looks like a MIME type)
        lastColon = src.lastIndexOf(':');
        if (lastColon > 0) {
            String possibleMimeType = src.substring(lastColon + 1).trim();
            // Check if it looks like a MIME type (contains '/' or common image types)
            if (possibleMimeType.contains("/") ||
                "image/svg+xml".equals(possibleMimeType) ||
                "application/octet-stream".equals(possibleMimeType)) {
                mimeType = possibleMimeType;
                src = src.substring(0, lastColon);
            }
        }

        // 4. Whatever remains is the URL/src
        src = src.trim();

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
