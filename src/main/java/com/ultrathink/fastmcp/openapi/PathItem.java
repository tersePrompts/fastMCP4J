package com.ultrathink.fastmcp.openapi;

import lombok.Value;

@Value
public class PathItem {
    Operation get;
    Operation post;
}
