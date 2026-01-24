package io.github.fastmcp.model;

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
}