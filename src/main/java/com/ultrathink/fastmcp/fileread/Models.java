package com.ultrathink.fastmcp.fileread;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Result from a grep/search operation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrepResult {
    /**
     * The path of the matching file.
     */
    private final String path;

    /**
     * The line numbers and content of matching lines.
     * Key is line number (1-indexed), value is line content.
     */
    private final java.util.Map<Integer, String> matches;

    /**
     * The total count of matches.
     */
    private final Integer count;

    public GrepResult(String path, java.util.Map<Integer, String> matches, Integer count) {
        this.path = path;
        this.matches = matches;
        this.count = count;
    }

    public String getPath() {
        return path;
    }

    public java.util.Map<Integer, String> getMatches() {
        return matches;
    }

    public Integer getCount() {
        return count;
    }
}

/**
 * Statistics about a file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileStats {
    /**
     * The path of the file.
     */
    private final String path;

    /**
     * Total number of lines.
     */
    private final long lineCount;

    /**
     * Total number of characters.
     */
    private final long charCount;

    /**
     * File size in bytes.
     */
    private final long sizeBytes;

    /**
     * The MIME type of the file (if detectable).
     */
    private final String mimeType;

    /**
     * Whether the file is binary.
     */
    private final boolean isBinary;

    public FileStats(String path, long lineCount, long charCount, long sizeBytes, String mimeType, boolean isBinary) {
        this.path = path;
        this.lineCount = lineCount;
        this.charCount = charCount;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.isBinary = isBinary;
    }

    public String getPath() {
        return path;
    }

    public long getLineCount() {
        return lineCount;
    }

    public long getCharCount() {
        return charCount;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isBinary() {
        return isBinary;
    }
}

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
