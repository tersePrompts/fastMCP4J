package io.github.fastmcp.notification;

public class LoggingNotification {
    private final String level;
    private final String logger;
    private final String data;
    
    public LoggingNotification(String level, String logger, String data) {
        this.level = level;
        this.logger = logger;
        this.data = data;
    }
    
    public String level() { return level; }
    public String logger() { return logger; }
    public String data() { return data; }
}
