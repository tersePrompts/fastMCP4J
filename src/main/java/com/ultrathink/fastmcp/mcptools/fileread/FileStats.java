package com.ultrathink.fastmcp.mcptools.fileread;

import com.fasterxml.jackson.annotation.JsonInclude;

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