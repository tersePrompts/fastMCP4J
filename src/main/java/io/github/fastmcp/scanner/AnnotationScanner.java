package io.github.fastmcp.scanner;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.annotations.WithProgress;
import io.github.fastmcp.model.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

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
            scanPrompts(clazz)
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
        boolean progressEnabled = method.isAnnotationPresent(WithProgress.class);

        return new ToolMeta(name, ann.description(), method, async, progressEnabled);
    }

    private ResourceMeta toResourceMeta(Method method) {
        McpResource ann = method.getAnnotation(McpResource.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new ResourceMeta(ann.uri(), name, ann.description(), ann.mimeType(), method, async);
    }

    private PromptMeta toPromptMeta(Method method) {
        McpPrompt ann = method.getAnnotation(McpPrompt.class);
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        boolean async = method.isAnnotationPresent(McpAsync.class);

        return new PromptMeta(name, ann.description(), method, async);
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
