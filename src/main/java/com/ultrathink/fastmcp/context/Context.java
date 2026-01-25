package com.ultrathink.fastmcp.context;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Context provides access to MCP capabilities within tool, resource, and prompt handlers.
 * Each MCP request receives a new context instance scoped to that request.
 * 
 * Capabilities:
 * - Logging: debug(), info(), warning(), error()
 * - Progress: reportProgress()
 * - LLM Sampling: sample() (future feature)
 * - Resource Access: listResources(), readResource()
 * - Prompt Access: listPrompts(), getPrompt()
 * - Session State: setState(), getState(), deleteState()
 * - Request Info: getRequestId(), getClientId(), getSessionId()
 * - Transport: getTransport()
 * - Server Info: getServerName()
 */
public interface Context {
    
    /**
     * Log a debug message to the client.
     * @param message Debug message
     */
    void debug(String message);
    
    /**
     * Log an info message to the client.
     * @param message Info message
     */
    void info(String message);
    
    /**
     * Log a warning message to the client.
     * @param message Warning message
     */
    void warning(String message);
    
    /**
     * Log an error message to the client.
     * @param message Error message
     */
    void error(String message);
    
    /**
     * Report progress for long-running operations.
     * @param progress Current progress value
     * @param total Total value (e.g., 50 out of 100 = 50%)
     */
    void reportProgress(double progress, double total);
    
    /**
     * Report progress with a message.
     * @param progress Current progress value
     * @param total Total value
     * @param message Progress message
     */
    void reportProgress(double progress, double total, String message);
    
    /**
     * List all available resources.
     * @return List of resource metadata
     */
    List<ResourceInfo> listResources();
    
    /**
     * Read a specific resource by URI.
     * @param uri Resource URI
     * @return Resource content
     */
    String readResource(String uri);
    
    /**
     * List all available prompts.
     * @return List of prompt metadata
     */
    List<PromptInfo> listPrompts();
    
    /**
     * Get a specific prompt with arguments.
     * @param name Prompt name
     * @param arguments Optional prompt arguments
     * @return Prompt result with messages
     */
    PromptResult getPrompt(String name, Map<String, Object> arguments);
    
    /**
     * Store a value in session state.
     * Session state persists across requests within the same MCP session.
     * @param key State key
     * @param value State value
     */
    void setState(String key, Object value);
    
    /**
     * Retrieve a value from session state.
     * @param key State key
     * @return Stored value, or null if not found
     */
    Object getState(String key);
    
    /**
     * Remove a value from session state.
     * @param key State key
     */
    void deleteState(String key);
    
    /**
     * Get the unique ID for the current MCP request.
     * @return Request ID
     */
    String getRequestId();
    
    /**
     * Get the client ID if provided during initialization.
     * @return Client ID, or null if not available
     */
    String getClientId();
    
    /**
     * Get the MCP session ID.
     * @return Session ID
     * @throws IllegalStateException if MCP session is not established
     */
    String getSessionId();
    
    /**
     * Get the transport type being used.
     * @return Transport type ("stdio", "sse", "streamable-http"), or null if not available
     */
    String getTransport();
    
    /**
     * Get the server name.
     * @return Server name
     */
    String getServerName();
    
    /**
     * Resource metadata information.
     */
    class ResourceInfo {
        private final String uri;
        private final String name;
        private final String description;
        private final String mimeType;
        
        public ResourceInfo(String uri, String name, String description, String mimeType) {
            this.uri = uri;
            this.name = name;
            this.description = description;
            this.mimeType = mimeType;
        }
        
        public String getUri() { return uri; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMimeType() { return mimeType; }
    }
    
    /**
     * Prompt metadata information.
     */
    class PromptInfo {
        private final String name;
        private final String description;
        private final List<String> arguments;
        
        public PromptInfo(String name, String description, List<String> arguments) {
            this.name = name;
            this.description = description;
            this.arguments = arguments;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getArguments() { return arguments; }
    }
    
    /**
     * Prompt result with messages.
     */
    class PromptResult {
        private final List<PromptMessage> messages;
        
        public PromptResult(List<PromptMessage> messages) {
            this.messages = messages;
        }
        
        public List<PromptMessage> getMessages() { return messages; }
    }
    
    /**
     * A single prompt message.
     */
    class PromptMessage {
        private final String role;
        private final String content;
        
        public PromptMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
