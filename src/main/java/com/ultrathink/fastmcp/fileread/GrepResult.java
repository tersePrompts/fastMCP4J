package com.ultrathink.fastmcp.fileread;

import com.fasterxml.jackson.annotation.JsonInclude;

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