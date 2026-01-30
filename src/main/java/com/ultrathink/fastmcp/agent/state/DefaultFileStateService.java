package com.ultrathink.fastmcp.agent.state;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default file-based state service with path traversal protection.
 */
public class DefaultFileStateService implements FileStateService {

    private final Path rootDirectory;
    private final String sessionPrefix;

    public DefaultFileStateService(Path rootDirectory) {
        this(rootDirectory, null);
    }

    private DefaultFileStateService(Path rootDirectory, String sessionPrefix) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        this.sessionPrefix = sessionPrefix;
        try {
            Files.createDirectories(this.rootDirectory);
        } catch (IOException e) {
            throw new StateException("Failed to create state directory: " + this.rootDirectory, e);
        }
    }

    @Override
    public void write(String path, String content, WriteMode mode) throws StateException {
        Path target = resolveSafe(path);
        try {
            Files.createDirectories(target.getParent());

            switch (mode) {
                case OVERWRITE -> Files.writeString(target, content, StandardCharsets.UTF_8);
                case APPEND -> Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                case CREATE_ONLY -> {
                    if (Files.exists(target)) {
                        throw new StateException(path, "File already exists: " + path);
                    }
                    Files.writeString(target, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
                }
            }
        } catch (IOException e) {
            throw new StateException(path, "Failed to write file: " + path, e);
        }
    }

    @Override
    public String read(String path) throws StateException {
        Path target = resolveSafe(path);
        if (!Files.exists(target)) {
            throw new StateException(path, "File not found: " + path);
        }
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StateException(path, "Failed to read file: " + path, e);
        }
    }

    @Override
    public Optional<String> readOptional(String path) {
        try {
            return Optional.of(read(path));
        } catch (StateException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolveSafe(path));
    }

    @Override
    public void delete(String path) throws StateException {
        Path target = resolveSafe(path);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new StateException(path, "Failed to delete file: " + path, e);
        }
    }

    @Override
    public List<String> list(String path) {
        Path target = resolveSafe(path.isEmpty() ? "." : path);
        if (!Files.exists(target)) {
            return List.of();
        }
        if (!Files.isDirectory(target)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(target)) {
            return stream
                .filter(p -> !Files.isDirectory(p))
                .map(p -> rootDirectory.relativize(p).toString())
                .toList();
        } catch (IOException e) {
            throw new StateException(path, "Failed to list directory: " + path, e);
        }
    }

    @Override
    public List<String> listAll() {
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.filter(p -> !Files.isDirectory(p))
                .map(p -> rootDirectory.relativize(p).toString())
                .forEach(result::add);
        } catch (IOException e) {
            throw new StateException("", "Failed to list all files", e);
        }
        return result;
    }

    @Override
    public void replace(String path, String oldText, String newText) throws StateException {
        String content = read(path);
        if (!content.contains(oldText)) {
            throw new StateException(path, "Text not found in file: " + path);
        }
        String newContent = content.replace(oldText, newText);
        write(path, newContent, WriteMode.OVERWRITE);
    }

    @Override
    public FileStateService forSession(String sessionId) {
        String safeSessionId = sanitize(sessionId);
        return new DefaultFileStateService(
            rootDirectory.resolve("sessions").resolve(safeSessionId),
            safeSessionId
        );
    }

    @Override
    public Path resolveSafe(String path) throws StateException {
        if (path == null || path.isEmpty()) {
            return rootDirectory;
        }

        // Prevent path traversal
        if (path.contains("..") || path.contains("\\..") || path.contains("/..")) {
            throw new StateException(path, "Path traversal not allowed: " + path);
        }

        // Sanitize path
        String sanitized = path.replace("\\", "/");

        Path resolved = rootDirectory.resolve(sanitized).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new StateException(path, "Path escapes state directory: " + path);
        }

        return resolved;
    }

    @Override
    public Path getRootDirectory() {
        return rootDirectory;
    }

    private String sanitize(String sessionId) {
        // Remove any path separators and special characters
        return sessionId.replaceAll("[/\\\\\\.]", "_");
    }
}
