package io.modelcontextprotocol.sdk;

import java.util.Map;

public class CallToolRequest {
    private final Map<String, Object> arguments;
    public CallToolRequest(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    public Map<String, Object> arguments() { return arguments; }
}
