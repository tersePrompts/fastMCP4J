package com.ultrathink.fastmcp.openapi;

import lombok.Value;

import java.util.Map;

@Value
public class Operation {
    String summary;
    String description;
    Map<String, Object> parameters;
    Object requestBody;
    Map<String, String> responses;
}
