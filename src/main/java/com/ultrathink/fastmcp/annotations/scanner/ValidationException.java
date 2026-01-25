package com.ultrathink.fastmcp.annotations.scanner;

import com.ultrathink.fastmcp.exception.FastMcpException;

public class ValidationException extends FastMcpException {
    public ValidationException(String message) {
        super(message);
    }
}