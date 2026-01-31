package io.github.terseprompts.fastmcp.mcptools.fileread;

/**
 * Exception thrown by FileRead operations.
 */
public class FileReadException extends RuntimeException {
    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
