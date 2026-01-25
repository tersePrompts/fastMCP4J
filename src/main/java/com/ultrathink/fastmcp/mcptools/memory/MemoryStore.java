package com.ultrathink.fastmcp.mcptools.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for memory storage backends.
 * Implementations can store memory in-memory, on-disk, in a database, or any other storage mechanism.
 * <p>
 * All methods must be thread-safe for concurrent access.
 */
public interface MemoryStore {

    /**
     * Get the root path for this memory store (e.g., "/memories")
     */
    String getRootPath();

    /**
     * List contents of a directory.
     *
     * @param path the directory path (relative to root)
     * @return list of directory entries with metadata
     * @throws MemoryException if the path doesn't exist or isn't a directory
     */
    List<MemoryEntry> list(String path) throws MemoryException;

    /**
     * Check if a path exists.
     *
     * @param path the path to check (relative to root)
     * @return true if the path exists, false otherwise
     */
    boolean exists(String path);

    /**
     * Get file contents.
     *
     * @param path the file path (relative to root)
     * @param viewRange optional [startLine, endLine] (1-indexed), null for full content
     * @return the file contents with line numbers
     * @throws MemoryException if the path doesn't exist, isn't a file, or exceeds line limit
     */
    FileContent read(String path, Optional<int[]> viewRange) throws MemoryException;

    /**
     * Create a new file with content.
     *
     * @param path the file path (relative to root)
     * @param content the file content
     * @throws MemoryException if the file already exists or parent directory doesn't exist
     */
    void create(String path, String content) throws MemoryException;

    /**
     * Replace text in a file.
     *
     * @param path the file path (relative to root)
     * @param oldStr the text to replace (must appear exactly once)
     * @param newStr the replacement text
     * @throws MemoryException if the file doesn't exist, text not found, or appears multiple times
     */
    void replace(String path, String oldStr, String newStr) throws MemoryException;

    /**
     * Insert text at a specific line.
     *
     * @param path the file path (relative to root)
     * @param insertLine the line number to insert at (1-indexed, 0 = before first line)
     * @param insertText the text to insert
     * @throws MemoryException if the file doesn't exist or line number is invalid
     */
    void insert(String path, int insertLine, String insertText) throws MemoryException;

    /**
     * Delete a file or directory.
     *
     * @param path the path to delete (relative to root)
     * @throws MemoryException if the path doesn't exist
     */
    void delete(String path) throws MemoryException;

    /**
     * Rename or move a file/directory.
     *
     * @param oldPath the current path (relative to root)
     * @param newPath the new path (relative to root)
     * @throws MemoryException if the old path doesn't exist or new path already exists
     */
    void rename(String oldPath, String newPath) throws MemoryException;

    /**
     * Get metadata about a path.
     *
     * @param path the path (relative to root)
     * @return optional memory entry
     */
    Optional<MemoryEntry> getMetadata(String path);

    /**
     * Clear all memory (for testing or cleanup).
     */
    void clear();

    /**
     * Represents a file or directory entry.
     */
    record MemoryEntry(
        String path,
        boolean isDirectory,
        long size,
        long lastModified,
        Optional<String> contentType
    ) {}

    /**
     * Represents file contents with line numbers.
     */
    record FileContent(
        String path,
        List<String> lines,
        Map<Integer, String> lineNumbersToContent
    ) {}
}
