package io.github.terseprompts.fastmcp.model;

import io.github.terseprompts.fastmcp.icons.Icon;
import lombok.Data;

import java.util.List;

@Data
public class ServerMeta {
    String name;
    String version;
    String instructions;
    List<ToolMeta> tools;
    List<ResourceMeta> resources;
    List<PromptMeta> prompts;
    List<Icon> icons;

    public ServerMeta() {}

    public ServerMeta(String name, String version, String instructions, List<ToolMeta> tools, List<ResourceMeta> resources, List<PromptMeta> prompts) {
        this.name = name;
        this.version = version;
        this.instructions = instructions;
        this.tools = tools;
        this.resources = resources;
        this.prompts = prompts;
    }

    public ServerMeta(String name, String version, String instructions, List<ToolMeta> tools, List<ResourceMeta> resources, List<PromptMeta> prompts, List<Icon> icons) {
        this.name = name;
        this.version = version;
        this.instructions = instructions;
        this.tools = tools;
        this.resources = resources;
        this.prompts = prompts;
        this.icons = icons;
    }
}