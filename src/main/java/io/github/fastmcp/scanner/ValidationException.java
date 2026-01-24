package io.github.fastmcp.scanner;

import io.github.fastmcp.exception.FastMcpException;

public class ValidationException extends FastMcpException {
    public ValidationException(String message) {
        super(message);
    }
}