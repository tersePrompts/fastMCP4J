package io.github.fastmcp.openapi;

import lombok.Value;

@Value
public class PathItem {
    Operation get;
    Operation post;
}
