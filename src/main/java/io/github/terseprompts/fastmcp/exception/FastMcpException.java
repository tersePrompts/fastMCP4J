package io.github.terseprompts.fastmcp.exception;

public class FastMcpException extends RuntimeException {
    public FastMcpException(String message) {
        super(message);
    }

    public FastMcpException(String message, Throwable cause) {
        super(message, cause);
    }
}