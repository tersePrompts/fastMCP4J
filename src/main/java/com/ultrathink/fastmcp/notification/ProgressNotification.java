package com.ultrathink.fastmcp.notification;

import lombok.Data;

@Data
public class ProgressNotification {
    double progressToken;
    double progress;
    String message;
    
    public ProgressNotification() {}
    
    public ProgressNotification(double progressToken, double progress, String message) {
        this.progressToken = progressToken;
        this.progress = progress;
        this.message = message;
    }
}
