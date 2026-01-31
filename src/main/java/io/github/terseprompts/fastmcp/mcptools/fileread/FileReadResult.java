package io.github.terseprompts.fastmcp.mcptools.fileread;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result from reading file lines.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileReadResult {
    /**
     * The path of the file.
     */
    private final String path;

    /**
     * The lines read from the file.
     * Key is line number (1-indexed), value is line content.
     */
    private final java.util.Map<Integer, String> lines;

    /**
     * The total number of lines in the file.
     */
    private final long totalLines;

    /**
     * Whether the file is binary (cannot be read as text).
     */
    private final Boolean isBinary;

    public FileReadResult(String path, java.util.Map<Integer, String> lines, long totalLines, Boolean isBinary) {
        this.path = path;
        this.lines = lines;
        this.totalLines = totalLines;
        this.isBinary = isBinary;
    }

    public String getPath() {
        return path;
    }

    public java.util.Map<Integer, String> getLines() {
        return lines;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public Boolean isBinary() {
        return isBinary;
    }
}