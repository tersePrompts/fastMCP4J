package com.ultrathink.fastmcp.notification;

import lombok.Value;

@Value
public class ProgressNotification {
    double progressToken;
    double progress;
    String message;
}
