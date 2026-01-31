package io.github.terseprompts.fastmcp.annotations.scanner;

import io.github.terseprompts.fastmcp.exception.FastMcpException;

public class ValidationException extends FastMcpException {
    public ValidationException(String message) {
        super(message);
    }
}