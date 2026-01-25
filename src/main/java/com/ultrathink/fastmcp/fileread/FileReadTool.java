package com.ultrathink.fastmcp.fileread;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * FileReadTool provides operations for reading and searching files.
 * 
 * Based on Claude Code's file tools pattern:
 * - Read specific lines with context (read_lines)
 * - Grep/search across files (grep)
 * - Read entire files (read_file)
 * - Get file statistics (get_stats)
 * 
 * Security considerations:
 * - No file writing operations
 * - No directory traversal (path validation)
 * - File existence checks before operations
 */
@McpTool(name = "fileread")
public class FileReadTool {
    
    private static final int MAX_LINES = 1000000; // Prevent reading extremely large files
    private static final int MAX_RESULT_SIZE = 1000000; // Prevent excessive output
    
    /**
     * Reads a range of lines from a file.
     * 
     * @param path The path to the file (relative or absolute)
     * @param startLine The starting line number (1-indexed, inclusive)
     * @param endLine The ending line number (1-indexed, inclusive), or null for end of file
     * @return Map with line numbers to content
     */
    @McpTool(name = "read_lines", description = "Read a range of lines from a file. Lines are 1-indexed.")
    public FileReadResult readLines(
        @McpParam(description = "The path to the file", required = true)
        String path,
        
        @McpParam(description = "The starting line number (1-indexed)", required = true)
        int startLine,
        
        @McpParam(description = "The ending line number (1-indexed, or null for end of file)", required = false)
        Integer endLine
    ) {
        Path filePath = Paths.get(path).normalize();
        
        // Validate path exists and is file
        if (!Files.exists(filePath)) {
            throw new FileReadException("File not found: " + path);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new FileReadException("Not a file: " + path);
        }
        
        // Check if binary file
        if (isBinaryFile(filePath)) {
            return new FileReadResult(path, null, 0, true);
        }
        
        try {
            List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            int totalLines = allLines.size();
            
            // Clamp to file bounds
            if (startLine < 1) startLine = 1;
            if (startLine > totalLines) {
                startLine = totalLines + 1;
                endLine = startLine - 1;
            }
            if (endLine == null || endLine > totalLines) {
                endLine = totalLines;
            }
            if (endLine < startLine) {
                endLine = startLine;
            }
            
            // Extract the requested range
            Map<Integer, String> resultLines = new LinkedHashMap<>();
            for (int i = startLine - 1; i < endLine; i++) {
                resultLines.put(i + 1, allLines.get(i));
            }
            
            return new FileReadResult(path, resultLines, totalLines, false);
            
        } catch (IOException e) {
            throw new FileReadException("Failed to read file: " + path, e);
        }
    }
    
    /**
     * Searches for a pattern in a file.
     * 
     * @param path The path to the file or directory
     * @param pattern The regex pattern to search for
     * @param outputMode 'content' for matching lines, 'files_with_matches' for file names, 'count' for match count
     * @param caseInsensitive Whether to ignore case
     * @param linesBefore Number of lines before match (content mode only)
     * @param linesAfter Number of lines after match (content mode only)
     * @param maxMatches Maximum number of matches to return
     * @return List of GrepResult objects
     */
    @McpTool(name = "grep", description = "Search for a regex pattern in a file")
    public List<GrepResult> grep(
        @McpParam(description = "The path to the file or directory to search in", required = true)
        String path,
        
        @McpParam(description = "The regex pattern to search for", required = true)
        String pattern,
        
        @McpParam(description = "Output mode: 'content' for lines, 'files_with_matches' for filenames, 'count' for match count", 
                  defaultValue = "content", required = false)
        String outputMode,
        
        @McpParam(description = "Case insensitive search", defaultValue = "false", required = false)
        boolean caseInsensitive,
        
        @McpParam(description = "Number of lines before each match (content mode only)", required = false)
        Integer linesBefore,
        
        @McpParam(description = "Number of lines after each match (content mode only)", required = false)
        Integer linesAfter,
        
        @McpParam(description = "Maximum number of matches to return", required = false)
        Integer maxMatches
    ) {
        Path searchPath = Paths.get(path).normalize();
        if (!Files.exists(searchPath)) {
            throw new FileReadException("Path not found: " + path);
        }
        
        List<Path> filesToSearch;
        try {
            if (Files.isRegularFile(searchPath)) {
                filesToSearch = List.of(searchPath);
            } else {
                filesToSearch = Files.walk(searchPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> !isBinaryFile(p))
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new FileReadException("Failed to search path: " + path, e);
        }
        
        // Compile regex pattern
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern regex;
        try {
            regex = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            throw new FileReadException("Invalid regex pattern: " + pattern, e);
        }
        
        List<GrepResult> results = new ArrayList<>();
        int matchLimit = maxMatches != null ? maxMatches : Integer.MAX_VALUE;
        int totalMatches = 0;
        
        for (Path filePath : filesToSearch) {
            if (totalMatches >= matchLimit) break;
            
            try {
                List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                Map<Integer, String> matches = new LinkedHashMap<>();
                int fileMatchCount = 0;
                
                for (int i = 0; i < lines.size(); i++) {
                    if (totalMatches >= matchLimit) break;
                    
                    Matcher matcher = regex.matcher(lines.get(i));
                    if (matcher.find()) {
                        fileMatchCount++;
                        totalMatches++;
                        
                        if ("content".equals(outputMode)) {
                            // Add context lines
                            int startContext = Math.max(0, i - (linesBefore != null ? linesBefore : 0));
                            int endContext = Math.min(lines.size() - 1, i + (linesAfter != null ? linesAfter : 0));
                            
                            for (int j = startContext; j <= endContext; j++) {
                                if (!matches.containsKey(j + 1)) {
                                    matches.put(j + 1, lines.get(j));
                                }
                            }
                        }
                    }
                }
                
                if (fileMatchCount > 0) {
                    String relativePath = searchPath.relativize(filePath).toString();
                    if ("files_with_matches".equals(outputMode)) {
                        results.add(new GrepResult(relativePath, null, null));
                    } else if ("content".equals(outputMode)) {
                        results.add(new GrepResult(relativePath, matches, null));
                    } else if ("count".equals(outputMode)) {
                        results.add(new GrepResult(relativePath, null, fileMatchCount));
                    }
                }
            } catch (IOException e) {
                // Skip files that can't be read
                continue;
            }
        }
        
        return results;
    }
    
    /**
     * Reads the entire contents of a file.
     * 
     * @param path The path to the file
     * @return Map with line numbers to content
     */
    @McpTool(name = "read_file", description = "Read the entire contents of a file")
    public FileReadResult readFile(
        @McpParam(description = "The path to the file", required = true)
        String path
    ) {
        Path filePath = Paths.get(path).normalize();
        
        if (!Files.exists(filePath)) {
            throw new FileReadException("File not found: " + path);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new FileReadException("Not a file: " + path);
        }
        
        // Check if binary file
        if (isBinaryFile(filePath)) {
            return new FileReadResult(path, null, 0, true);
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            
            if (lines.size() > MAX_LINES) {
                throw new FileReadException("File too large to read at once: " + lines.size() + " lines. Use read_lines with range instead.");
            }
            
            Map<Integer, String> lineMap = new LinkedHashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                lineMap.put(i + 1, lines.get(i));
            }
            
            return new FileReadResult(path, lineMap, lines.size(), false);
            
        } catch (IOException e) {
            throw new FileReadException("Failed to read file: " + path, e);
        }
    }
    
    /**
     * Gets statistics about a file.
     * 
     * @param path The path to the file
     * @return FileStats object with file information
     */
    @McpTool(name = "get_stats", description = "Get statistics about a file (line count, size, etc.)")
    public FileStats getStats(
        @McpParam(description = "The path to the file", required = true)
        String path
    ) {
        Path filePath = Paths.get(path).normalize();
        
        if (!Files.exists(filePath)) {
            throw new FileReadException("File not found: " + path);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new FileReadException("Not a file: " + path);
        }
        
        try {
            long sizeBytes = Files.size(filePath);
            String mimeType = guessMimeType(filePath);
            boolean isBinary = isBinaryFile(filePath);
            
            long lineCount = 0;
            long charCount = 0;
            
            if (!isBinary) {
                try {
                    List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                    lineCount = lines.size();
                    charCount = lines.stream().mapToLong(String::length).sum();
                } catch (IOException e) {
                    // Can't read as text
                }
            }
            
            return new FileStats(path, lineCount, charCount, sizeBytes, mimeType, isBinary);
            
        } catch (IOException e) {
            throw new FileReadException("Failed to get file stats: " + path, e);
        }
    }
    
    // Helper methods
    
    private boolean isBinaryFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead = in.read(buffer);
            
            if (bytesRead <= 0) return false;
            
            // Check for binary content
            int nullCount = 0;
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == 0) nullCount++;
                if (buffer[i] < 32 && buffer[i] != 9 && buffer[i] != 10 && buffer[i] != 13) {
                    return true; // Control character (not tab, newline, or carriage return)
                }
            }
            
            // More than 1 null byte suggests binary
            if (nullCount > 1) return true;
            
            return false;
        } catch (IOException e) {
            return true; // Assume binary if can't read
        }
    }
    
    private String guessMimeType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".java")) return "text/x-java-source";
        if (fileName.endsWith(".xml")) return "text/xml";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".md")) return "text/markdown";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".py")) return "text/x-python";
        if (fileName.endsWith(".js")) return "text/javascript";
        if (fileName.endsWith(".ts")) return "text/typescript";
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".csv")) return "text/csv";
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return "text/yaml";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        
        return "application/octet-stream";
    }
}
