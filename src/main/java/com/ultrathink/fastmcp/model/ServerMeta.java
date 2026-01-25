package com.ultrathink.fastmcp.model;

import com.ultrathink.fastmcp.icons.Icon;
import lombok.Value;

import java.util.List;

@Value
public class ServerMeta {
    String name;
    String version;
    String instructions;
    List<ToolMeta> tools;
    List<ResourceMeta> resources;
    List<PromptMeta> prompts;
    List<Icon> icons;
}