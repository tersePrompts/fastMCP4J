package io.github.terseprompts.fastmcp.mcptools.filewrite;

/**
 * Exception thrown when file write operations fail.
 */
public class FileWriteException extends RuntimeException {
    public FileWriteException(String message) {
        super(message);
    }

    public FileWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
