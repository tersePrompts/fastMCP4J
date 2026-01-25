package com.ultrathink.fastmcp.notification;

import lombok.Data;

@Data
public class LoggingNotification {
    String level;
    String logger;
    String data;
    
    public LoggingNotification() {}
    
    public LoggingNotification(String level, String logger, String data) {
        this.level = level;
        this.logger = logger;
        this.data = data;
    }
}
