package io.github.terseprompts.fastmcp.mcptools.todo;

/**
 * Exception thrown for todo-related errors.
 */
public class TodoException extends RuntimeException {

    public TodoException(String message) {
        super(message);
    }

    public TodoException(String message, Throwable cause) {
        super(message, cause);
    }
}
