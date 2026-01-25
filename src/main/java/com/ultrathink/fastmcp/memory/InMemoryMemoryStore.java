package com.ultrathink.fastmcp.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory implementation of MemoryStore.
 * Uses ConcurrentHashMap for storage and ReadWriteLock for granular locking.
 */
public class InMemoryMemoryStore implements MemoryStore {

    private static final int MAX_LINES = 999_999;
    private static final int DEFAULT_MAX_FILE_SIZE = 10_000_000; // 10MB

    private final String rootPath;
    private final int maxFileSize;
    private final Map<String, MemoryEntry> files;
    private final Map<String, String> fileContents;
    private final ReadWriteLock globalLock;

    public InMemoryMemoryStore() {
        this("/memories");
    }

    public InMemoryMemoryStore(String rootPath) {
        this(rootPath, DEFAULT_MAX_FILE_SIZE);
    }

    public InMemoryMemoryStore(String rootPath, int maxFileSize) {
        this.rootPath = normalizePath(rootPath);
        this.maxFileSize = maxFileSize;
        this.files = new ConcurrentHashMap<>();
        this.fileContents = new ConcurrentHashMap<>();
        this.globalLock = new ReentrantReadWriteLock();
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public List<MemoryEntry> list(String path) throws MemoryException {
        path = normalizePath(path);

        globalLock.readLock().lock();
        try {
            // Check if the path is a directory (it's a directory if any files start with this path + "/")
            boolean isDir = files.values().stream()
                .anyMatch(entry -> {
                    String entryPath = entry.path();
                    if (entryPath.equals(path)) return false;
                    return entryPath.startsWith(path + "/");
                });

            if (!isDir && !files.containsKey(path)) {
                throw new MemoryException("Path does not exist: " + path);
            }

            List<MemoryEntry> entries = new ArrayList<>();

            // Find immediate children
            files.values().forEach(entry -> {
                String entryPath = entry.path();
                if (entryPath.equals(path)) return;

                if (entryPath.startsWith(path + "/")) {
                    String relative = entryPath.substring(path.length() + 1);
                    String firstSegment = relative.contains("/")
                        ? relative.substring(0, relative.indexOf("/"))
                        : relative;

                    String childPath = path.isEmpty() ? firstSegment : path + "/" + firstSegment;

                    // Check if we've already added this directory
                    if (entries.stream().noneMatch(e -> e.path().equals(childPath))) {
                        // Check if it's a directory
                        boolean childIsDir = files.values().stream()
                            .anyMatch(e -> e.path().startsWith(childPath + "/") && !e.path().equals(childPath));

                        long size = childIsDir
                            ? files.values().stream()
                                .filter(e -> e.path().startsWith(childPath + "/") || e.path().equals(childPath))
                                .mapToLong(MemoryEntry::size)
                                .sum()
                            : entry.size();

                        long lastModified = childIsDir
                            ? files.values().stream()
                                .filter(e -> e.path().startsWith(childPath + "/") || e.path().equals(childPath))
                                .mapToLong(MemoryEntry::lastModified)
                                .max()
                                .orElse(Instant.now().toEpochMilli())
                            : entry.lastModified();

                        entries.add(new MemoryEntry(
                            childPath,
                            childIsDir,
                            size,
                            lastModified,
                            Optional.empty()
                        ));
                    }
                }
            });

            // Sort by path
            entries.sort(Comparator.comparing(MemoryEntry::path));

            return entries;
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public boolean exists(String path) {
        path = normalizePath(path);
        globalLock.readLock().lock();
        try {
            if (files.containsKey(path)) {
                return true;
            }
            // Check if it's a directory
            return files.values().stream()
                .anyMatch(entry -> entry.path().startsWith(path + "/"));
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public FileContent read(String path, Optional<int[]> viewRange) throws MemoryException {
        path = normalizePath(path);

        globalLock.readLock().lock();
        try {
            MemoryEntry entry = files.get(path);
            if (entry == null) {
                throw new MemoryException("File not found: " + path);
            }
            if (entry.isDirectory()) {
                throw new MemoryException("Cannot read directory as file: " + path);
            }

            String content = fileContents.get(path);
            if (content == null) {
                throw new MemoryException("File content not found: " + path);
            }

            List<String> lines = List.of(content.split("\n", -1));

            if (lines.size() > MAX_LINES) {
                throw new MemoryException(
                    String.format("File %s exceeds maximum line limit of %d lines.", path, MAX_LINES)
                );
            }

            Map<Integer, String> lineNumbersToContent = new java.util.HashMap<>();

            int[] range = viewRange.orElse(null);
            int startLine = (range != null && range.length == 2) ? Math.max(1, range[0]) : 1;
            int endLine = (range != null && range.length == 2) ? Math.min(lines.size(), range[1]) : lines.size();

            for (int i = startLine - 1; i < endLine; i++) {
                lineNumbersToContent.put(i + 1, lines.get(i));
            }

            return new FileContent(path, lines, lineNumbersToContent);
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public void create(String path, String content) throws MemoryException {
        path = normalizePath(path);

        globalLock.writeLock().lock();
        try {
            if (files.containsKey(path)) {
                throw new MemoryException("File already exists: " + path);
            }

            // Check parent directory exists
            String parentPath = getParentPath(path);
            if (!parentPath.isEmpty() && !exists(parentPath)) {
                throw new MemoryException("Parent directory does not exist: " + parentPath);
            }

            long size = content.getBytes().length;
            if (size > maxFileSize) {
                throw new MemoryException(
                    String.format("File size %d exceeds maximum allowed size of %d bytes", size, maxFileSize)
                );
            }

            MemoryEntry entry = new MemoryEntry(
                path,
                false,
                size,
                Instant.now().toEpochMilli(),
                Optional.empty()
            );

            files.put(path, entry);
            fileContents.put(path, content);
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public void replace(String path, String oldStr, String newStr) throws MemoryException {
        path = normalizePath(path);

        globalLock.writeLock().lock();
        try {
            String content = fileContents.get(path);
            if (content == null) {
                throw new MemoryException("File not found: " + path);
            }

            if (!content.contains(oldStr)) {
                throw new MemoryException(
                    String.format("Text to replace not found in file: %s", path)
                );
            }

            // Check for multiple occurrences
            long count = countOccurrences(content, oldStr);
            if (count > 1) {
                // Find line numbers of all occurrences
                List<Integer> lineNumbers = findOccurrenceLines(content, oldStr);
                throw new MemoryException(
                    String.format(
                        "Multiple occurrences of old_str `%s` in lines: %s. Please ensure it is unique.",
                        oldStr,
                        lineNumbers
                    )
                );
            }

            String newContent = content.replace(oldStr, newStr);
            fileContents.put(path, newContent);

            // Update entry metadata
            MemoryEntry entry = files.get(path);
            if (entry != null) {
                files.put(path, new MemoryEntry(
                    entry.path(),
                    entry.isDirectory(),
                    newContent.getBytes().length,
                    Instant.now().toEpochMilli(),
                    entry.contentType()
                ));
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public void insert(String path, int insertLine, String insertText) throws MemoryException {
        path = normalizePath(path);

        globalLock.writeLock().lock();
        try {
            String content = fileContents.get(path);
            if (content == null) {
                throw new MemoryException("File not found: " + path);
            }

            List<String> lines = new ArrayList<>(List.of(content.split("\n", -1)));

            if (insertLine < 0 || insertLine > lines.size()) {
                throw new MemoryException(
                    String.format(
                        "Invalid `insert_line` parameter: %d. It should be within the range of lines of the file: [0, %d]",
                        insertLine,
                        lines.size()
                    )
                );
            }

            lines.add(insertLine, insertText);
            String newContent = String.join("\n", lines);
            fileContents.put(path, newContent);

            // Update entry metadata
            MemoryEntry entry = files.get(path);
            if (entry != null) {
                files.put(path, new MemoryEntry(
                    entry.path(),
                    entry.isDirectory(),
                    newContent.getBytes().length,
                    Instant.now().toEpochMilli(),
                    entry.contentType()
                ));
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String path) throws MemoryException {
        path = normalizePath(path);

        globalLock.writeLock().lock();
        try {
            if (!files.containsKey(path)) {
                throw new MemoryException("Path does not exist: " + path);
            }

            boolean isDirectory = files.get(path).isDirectory();

            if (isDirectory) {
                // Delete all files in the directory
                files.keySet().stream()
                    .filter(p -> p.startsWith(path + "/"))
                    .toList()
                    .forEach(fileContents::remove);
            } else {
                fileContents.remove(path);
            }

            files.remove(path);
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public void rename(String oldPath, String newPath) throws MemoryException {
        oldPath = normalizePath(oldPath);
        newPath = normalizePath(newPath);

        globalLock.writeLock().lock();
        try {
            if (!files.containsKey(oldPath)) {
                throw new MemoryException("Old path does not exist: " + oldPath);
            }

            if (files.containsKey(newPath)) {
                throw new MemoryException("New path already exists: " + newPath);
            }

            boolean isDirectory = files.get(oldPath).isDirectory();

            if (isDirectory) {
                // Rename all files in the directory
                List<String> keys = files.keySet().stream()
                    .filter(p -> p.startsWith(oldPath + "/") || p.equals(oldPath))
                    .toList();

                for (String key : keys) {
                    String newKey = key.equals(oldPath)
                        ? newPath
                        : newPath + key.substring(oldPath.length());

                    MemoryEntry entry = files.get(key);
                    files.remove(key);
                    files.put(newKey, new MemoryEntry(
                        newKey,
                        entry.isDirectory(),
                        entry.size(),
                        Instant.now().toEpochMilli(),
                        entry.contentType()
                    ));

                    if (!entry.isDirectory()) {
                        String content = fileContents.remove(key);
                        fileContents.put(newKey, content);
                    }
                }
            } else {
                MemoryEntry entry = files.remove(oldPath);
                files.put(newPath, new MemoryEntry(
                    newPath,
                    entry.isDirectory(),
                    entry.size(),
                    Instant.now().toEpochMilli(),
                    entry.contentType()
                ));

                String content = fileContents.remove(oldPath);
                fileContents.put(newPath, content);
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<MemoryEntry> getMetadata(String path) {
        path = normalizePath(path);
        globalLock.readLock().lock();
        try {
            return Optional.ofNullable(files.get(path));
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        globalLock.writeLock().lock();
        try {
            files.clear();
            fileContents.clear();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Remove leading slash if present for internal storage
        return path.replace("\\", "/").replaceAll("/+", "/").replaceAll("^/", "");
    }

    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf("/");
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }

    private long countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    private List<Integer> findOccurrenceLines(String content, String oldStr) {
        List<Integer> lines = new ArrayList<>();
        String[] contentLines = content.split("\n");
        for (int i = 0; i < contentLines.length; i++) {
            if (contentLines[i].contains(oldStr)) {
                lines.add(i + 1);
            }
        }
        return lines;
    }
}
