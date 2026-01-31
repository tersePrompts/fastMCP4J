package io.github.terseprompts.fastmcp.mcptools.filewrite;

import lombok.Data;

/**
 * Result of a file write operation.
 */
@Data
public class FileWriteResult {
    private final String path;
    private final long bytesWritten;
    private final int linesWritten;
    private final String operation;
    private final boolean created;

    public FileWriteResult(String path, long bytesWritten, int linesWritten, String operation, boolean created) {
        this.path = path;
        this.bytesWritten = bytesWritten;
        this.linesWritten = linesWritten;
        this.operation = operation;
        this.created = created;
    }

    @Override
    public String toString() {
        return String.format("%s '%s': %d bytes, %d lines%s",
            operation, path, bytesWritten, linesWritten,
            created ? " (created)" : " (updated)");
    }
}
