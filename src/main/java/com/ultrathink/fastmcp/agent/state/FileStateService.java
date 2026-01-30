package com.ultrathink.fastmcp.agent.state;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SPI for file-based state storage.
 * <p>
 * Provides a safe, session-isolated way to store and retrieve state.
 * Includes path traversal protection and session binding.
 */
public interface FileStateService {

    /**
     * Write state to a file.
     *
     * @param path The relative path within the state directory
     * @param content The content to write
     * @param mode The write mode
     * @throws StateException if the operation fails
     */
    void write(String path, String content, WriteMode mode) throws StateException;

    /**
     * Write state to a file with default OVERWRITE mode.
     */
    default void write(String path, String content) throws StateException {
        write(path, content, WriteMode.OVERWRITE);
    }

    /**
     * Read state from a file.
     *
     * @param path The relative path within the state directory
     * @return The file content
     * @throws StateException if the file doesn't exist or read fails
     */
    String read(String path) throws StateException;

    /**
     * Read state from a file, returning Optional.
     */
    Optional<String> readOptional(String path);

    /**
     * Check if a state file exists.
     */
    boolean exists(String path);

    /**
     * Delete a state file.
     */
    void delete(String path) throws StateException;

    /**
     * List state files in a directory.
     *
     * @param path The relative directory path (empty for root)
     * @return List of relative file paths
     */
    List<String> list(String path);

    /**
     * List all state files recursively.
     */
    List<String> listAll();

    /**
     * Replace content in a state file.
     *
     * @param path The file path
     * @param oldText The text to replace
     * @param newText The replacement text
     * @throws StateException if operation fails or oldText not found
     */
    void replace(String path, String oldText, String newText) throws StateException;

    /**
     * Get a session-bound state service.
     * All operations are isolated to the specified session.
     *
     * @param sessionId The session ID
     * @return A new FileStateService bound to the session
     */
    FileStateService forSession(String sessionId);

    /**
     * Resolve a safe absolute path from a relative path.
     * Prevents path traversal attacks.
     *
     * @param path The relative path
     * @return The resolved absolute path
     * @throws StateException if the path is invalid
     */
    Path resolveSafe(String path) throws StateException;

    /**
     * Get the root state directory.
     */
    Path getRootDirectory();

    /**
     * Write modes.
     */
    enum WriteMode {
        /** Overwrite existing file content */
        OVERWRITE,
        /** Append to existing file content */
        APPEND,
        /** Only write if file doesn't exist */
        CREATE_ONLY
    }
}
