package io.modelcontextprotocol.sdk;

import java.util.List;
import java.util.Collections;

public class CallToolResult {
    private final List<TextContent> content;
    private final boolean isError;

    private CallToolResult(Builder b) {
        this.content = b.content;
        this.isError = b.isError;
    }

    public static Builder builder() { return new Builder(); }
    public List<TextContent> getContent() { return content; }
    public boolean isError() { return isError; }

    public static class Builder {
        List<TextContent> content = Collections.emptyList();
        boolean isError = false;
        public Builder content(List<TextContent> c) { this.content = c; return this; }
        public Builder isError(boolean e) { this.isError = e; return this; }
        public CallToolResult build() { return new CallToolResult(this); }
    }
}
