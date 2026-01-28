package com.ultrathink.fastmcp.mcptools.bash;

/**
 * Operating system type for shell command execution.
 */
public enum OsType {
    /**
     * Windows operating system - uses cmd.exe or PowerShell
     */
    WINDOWS,

    /**
     * Linux operating system - uses bash/sh
     */
    LINUX,

    /**
     * macOS operating system - uses zsh/bash
     */
    MACOS,

    /**
     * Unknown or unsupported operating system
     */
    UNKNOWN
}
