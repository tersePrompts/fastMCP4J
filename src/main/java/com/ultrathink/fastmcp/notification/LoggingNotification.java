package com.ultrathink.fastmcp.notification;

import lombok.Value;

@Value
public class LoggingNotification {
    String level;
    String logger;
    String data;
}
