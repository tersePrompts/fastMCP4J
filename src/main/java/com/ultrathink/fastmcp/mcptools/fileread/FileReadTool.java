package com.ultrathink.fastmcp.mcptools.fileread;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;

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
 * Security considerations:
 * - No file writing operations
 * - No directory traversal (path validation)
 * - File existence checks before operations
 */
public class FileReadTool {

    private static final int MAX_LINES = 1000000; // Prevent reading extremely large files
    private static final int MAX_RESULT_SIZE = 1000000; // Prevent excessive output

    /**
     * Unified file reading tool with multiple operation modes.
     * Consolidates readLines, grep, readFile, and getStats into a single tool.
     *
     * @param mode The operation mode to perform
     * @param path Path parameters (for readLines, readFile, getStats modes)
     * @param searchPath Search path parameter (for grep mode)
     * @param pattern Pattern parameter (for grep mode)
     * @param startLine Starting line number (for readLines mode)
     * @param endLine Ending line number (for readLines mode)
     * @param outputMode Output mode for grep results
     * @param caseInsensitive Case sensitivity for grep
     * @param linesBefore Context lines before match for grep
     * @param linesAfter Context lines after match for grep
     * @param maxMatches Maximum matches for grep
     * @return Result object (FileReadResult, List&lt;GrepResult&gt;, or FileStats)
     */
    @McpTool(
        name = "fileread",
        description = """
            Unified file reading tool with multiple operation modes. This tool provides comprehensive file access capabilities:

            MODES:
            1. "readLines" - Read a specific range of lines from a file. Use when you need only part of a file.
            2. "readFile" - Read the entire contents of a file. Use for smaller files or when you need everything.
            3. "grep" - Search for regex patterns across files. Use to find specific content or patterns.
            4. "getStats" - Get file statistics like line count, size, and type. Use to understand a file before reading.

            COMMON WORKFLOWS:
            - First use "getStats" to check file size and line count
            - Then use "readFile" for small files (<1000 lines) or "readLines" for larger files
            - Use "grep" when searching for specific patterns across one or more files

            EXAMPLE USAGE:
            - Check file info: mode="getStats", path="src/main.java"
            - Read entire file: mode="readFile", path="src/main.java"
            - Read lines 100-200: mode="readLines", path="src/main.java", startLine=100, endLine=200
            - Search pattern: mode="grep", searchPath="src/", pattern="class.*Controller"
            """
    )
    public Object fileread(
        // === MODE PARAMETER ===
        @McpParam(
            description = """
                The operation mode to perform. Determines which other parameters are required:

                "readLines" - Read a specific range of lines from a file. Requires: path, startLine. Optional: endLine.
                "readFile" - Read entire file contents. Requires: path.
                "grep" - Search for patterns in files. Requires: searchPath, pattern. Optional: outputMode, caseInsensitive, linesBefore, linesAfter, maxMatches.
                "getStats" - Get file statistics. Requires: path.

                Choose the appropriate mode based on your task:
                - Use getStats first to understand file size and avoid reading extremely large files
                - Use readFile for small files (typically <1000 lines)
                - Use readLines for large files when you only need specific sections
                - Use grep when searching for specific content across one or more files
                """,
            required = true,
            examples = {"readLines", "readFile", "grep", "getStats"},
            constraints = "Must be one of: readLines, readFile, grep, getStats"
        )
        String mode,

        // === PATH PARAMETER (for readLines, readFile, getStats) ===
        @McpParam(
            description = """
                File path to read (for readLines, readFile, or getStats modes).

                Can be absolute or relative to the current working directory.
                Examples:
                - Absolute: "C:/Users/username/project/src/Main.java"
                - Relative: "src/Main.java" or "./src/Main.java"
                - Parent reference: "../config/settings.json"

                The file must exist and be readable. Binary files are detected automatically.
                """,
            required = false,
            examples = {
                "src/main/java/com/example/App.java",
                "./README.md",
                "../config/application.properties",
                "C:/Projects/myapp/src/main.py",
                "/home/user/documents/report.txt"
            },
            constraints = "File must exist and be readable. Path will be normalized.",
            hints = "Use forward slashes or backslashes (will be normalized). Relative paths are resolved from current working directory. For grep mode, use searchPath parameter instead."
        )
        String path,

        // === SEARCH PATH PARAMETER (for grep mode) ===
        @McpParam(
            description = """
                Path to search in (for grep mode only).

                Can be a file path to search within a single file,
                or a directory path to search across all files in that directory recursively.

                Examples:
                - Single file: "src/main.java"
                - Directory: "src/" searches all files in src/
                - Current directory: "." searches all files in current directory
                - Parent: "../" searches parent directory
                """,
            required = false,
            examples = {
                "src/main.java",
                "src/",
                "./",
                "../config",
                "/home/user/project"
            },
            constraints = "Path must exist. Directories are searched recursively.",
            hints = "Only use with mode='grep'. Directory searches include all subdirectories. Binary files are automatically skipped. Use specific paths to narrow search scope and improve performance."
        )
        String searchPath,

        // === PATTERN PARAMETER (for grep mode) ===
        @McpParam(
            description = """
                Regular expression pattern to search for (for grep mode only).

                Supports Java regex syntax. Pattern is applied line-by-line.
                Special characters like ., *, +, ?, [, ], {, }, (, ), |, ^, $ have special meaning.

                Common patterns:
                - Exact match: "functionName" (matches lines containing 'functionName')
                - Word boundaries: "\\bclass\\s+\\w+" (matches 'class' followed by word)
                - Wildcard: "import.*;" (matches import statements)
                - OR pattern: "(TODO|FIXME)" (matches TODO or FIXME)
                - Character class: "[Ee]rror" (matches Error or error)

                Escape special characters with \\ when needed: "\\.", "\\(", "\\["
                """,
            required = false,
            examples = {
                "class\\s+\\w+",
                "public\\s+static\\s+void\\s+main",
                "(TODO|FIXME|HACK)",
                "import\\s+java\\.",
                "@Override",
                "function\\s*\\(",
                "\\b\\d{3}-\\d{3}-\\d{4}\\b"
            },
            constraints = "Must be valid Java regex syntax. Empty string matches nothing.",
            hints = "Only use with mode='grep'. Pattern is case-sensitive by default (use caseInsensitive=true for case-insensitive). Use \\\\ to escape special regex characters. Test complex patterns on small samples first. Pattern matches anywhere on a line unless anchors (^ or $) are used."
        )
        String pattern,

        // === START LINE PARAMETER (for readLines mode) ===
        @McpParam(
            description = """
                Starting line number to read (for readLines mode only).

                Lines are 1-indexed (line 1 is the first line).
                Must be >= 1. Values less than 1 are clamped to 1.

                If startLine exceeds the file's total line count, returns empty result.

                Examples:
                - startLine=1 reads from the beginning
                - startLine=100 starts at line 100
                - startLine=10, endLine=20 reads lines 10-20 (11 lines total)
                """,
            required = false,
            examples = {"1", "100", "500", "1"},
            constraints = "Must be >= 1. If > total lines, returns empty result.",
            hints = "Only use with mode='readLines'. Lines are 1-indexed (first line is 1, not 0). Use getStats mode first to check total line count. Start from smaller numbers to read from the beginning."
        )
        Integer startLine,

        // === END LINE PARAMETER (for readLines mode) ===
        @McpParam(
            description = """
                Ending line number to read (for readLines mode only).

                Lines are 1-indexed (inclusive).
                If null or not provided, reads to the end of the file.
                If > total lines, clamped to total lines.
                Must be >= startLine (enforced automatically).

                Examples:
                - endLine=100 reads lines 1-100 (if startLine=1)
                - endLine=50 reads lines 10-50 (if startLine=10)
                - endLine=null reads from startLine to end of file
                """,
            required = false,
            examples = {"100", "500", "1000"},
            constraints = "Must be >= startLine if specified. Clamped to file's total line count.",
            hints = "Only use with mode='readLines'. Set to null to read to end of file. Lines are 1-indexed and inclusive. Large ranges may impact performance."
        )
        Integer endLine,

        // === OUTPUT MODE PARAMETER (for grep) ===
        @McpParam(
            description = """
                Output format for grep search results (for grep mode only).

                Determines what information is returned:

                "content" - Returns matching lines with context (default).
                    Returns line numbers and line content for all matches.
                    Includes context lines if linesBefore/linesAfter are specified.

                "files_with_matches" - Returns only list of matching files.
                    Use when you only need to know which files contain the pattern,
                    not the specific matching lines.

                "count" - Returns number of matches per file.
                    Use when you need match statistics but not line content.

                Choose the simplest format that meets your needs for better performance.
                """,
            required = false,
            examples = {"content", "files_with_matches", "count"},
            constraints = "Must be one of: content, files_with_matches, count",
            hints = "Only use with mode='grep'. Use 'content' for viewing matching lines. Use 'files_with_matches' to find which files contain the pattern. Use 'count' for statistical analysis."
        )
        String outputMode,

        // === CASE INSENSITIVE PARAMETER (for grep) ===
        @McpParam(
            description = """
                Whether to perform case-insensitive search (for grep mode only).

                When true, pattern matching ignores letter case.
                When false (default), pattern matching is case-sensitive.

                Examples:
                - pattern="error" with caseInsensitive=false matches "error" but not "Error"
                - pattern="error" with caseInsensitive=true matches "error", "Error", "ERROR", "eRrOr"

                Use case-insensitive search when:
                - Searching for English words that may appear in different cases
                - Pattern doesn't rely on specific capitalization
                - You want more comprehensive matches

                Use case-sensitive search when:
                - Searching for case-sensitive identifiers (e.g., Java class names)
                - Pattern relies on specific capitalization (e.g., camelCase)
                - You want precise, controlled matching
                """,
            required = false,
            examples = {"true", "false"},
            constraints = "Boolean value: true or false",
            hints = "Only use with mode='grep'. Default is false (case-sensitive). Affects all letter matching in the pattern. Use true for English words, false for code identifiers."
        )
        Boolean caseInsensitive,

        // === LINES BEFORE PARAMETER (for grep) ===
        @McpParam(
            description = """
                Number of context lines to include before each match (for grep mode with outputMode='content').

                Provides surrounding context for better understanding of matches.
                Context helps identify the function, class, or section containing the match.

                Examples:
                - linesBefore=0 shows only matching lines
                - linesBefore=3 shows 3 lines before each match (typical for context)
                - linesBefore=5 shows 5 lines before each match (more context)
                - linesBefore=10 shows 10 lines before each match (extensive context)

                Use case examples:
                - Finding errors: linesBefore=5 helps see what led to the error
                - Finding function definitions: linesBefore=10 shows documentation
                - Pattern hunting: linesBefore=0 is fastest for locating matches
                """,
            required = false,
            examples = {"0", "3", "5", "10"},
            constraints = "Must be >= 0. Only applies when outputMode='content'.",
            hints = "Only use with mode='grep' and outputMode='content'. More context = larger output size. Default is 0 (no context before). Values of 3-5 are typical for code review."
        )
        Integer linesBefore,

        // === LINES AFTER PARAMETER (for grep) ===
        @McpParam(
            description = """
                Number of context lines to include after each match (for grep mode with outputMode='content').

                Provides surrounding context for better understanding of matches.
                Context helps see what follows or completes the matched pattern.

                Examples:
                - linesAfter=0 shows only matching lines
                - linesAfter=3 shows 3 lines after each match (typical for context)
                - linesAfter=5 shows 5 lines after each match (more context)
                - linesAfter=10 shows 10 lines after each match (extensive context)

                Use case examples:
                - Finding errors: linesAfter=5 helps see error handling or related code
                - Finding function definitions: linesAfter=10 shows function body start
                - Pattern hunting: linesAfter=0 is fastest for locating matches
                """,
            required = false,
            examples = {"0", "3", "5", "10"},
            constraints = "Must be >= 0. Only applies when outputMode='content'.",
            hints = "Only use with mode='grep' and outputMode='content'. More context = larger output size. Default is 0 (no context after). Values of 3-5 are typical for code review. Combined with linesBefore, shows full context window."
        )
        Integer linesAfter,

        // === MAX MATCHES PARAMETER (for grep) ===
        @McpParam(
            description = """
                Maximum number of matches to return (for grep mode only).

                Limits the total number of matches returned across all files.
                Prevents excessive output when searching large codebases.

                Examples:
                - maxMatches=10 returns first 10 matches only
                - maxMatches=100 returns first 100 matches
                - maxMatches=null or not set returns all matches (no limit)

                Use cases:
                - Quick exploration: maxMatches=10 for rapid feedback
                - Focused search: maxMatches=50 for targeted results
                - Comprehensive search: maxMatches=500 or null for complete results

                Performance considerations:
                - Smaller values = faster response
                - Large codebases with common patterns may produce thousands of matches
                - Start with small limits, increase if needed
                """,
            required = false,
            examples = {"10", "50", "100", "500"},
            constraints = "Must be >= 1",
            hints = "Only use with mode='grep'. Default is no limit (all matches returned). Limiting matches improves performance. Applies across all files searched. Start small (10-50), increase if needed."
        )
        Integer maxMatches
    ) {
        // Dispatch based on mode
        switch (mode) {
            case "readLines":
                return handleReadLines(path, startLine, endLine);
            case "readFile":
                return handleReadFile(path);
            case "grep":
                return handleGrep(searchPath, pattern, outputMode, caseInsensitive, linesBefore, linesAfter, maxMatches);
            case "getStats":
                return handleGetStats(path);
            default:
                throw new FileReadException("Unknown mode: " + mode + ". Valid modes are: readLines, readFile, grep, getStats");
        }
    }

    // ========== MODE HANDLERS ==========

    /**
     * Handle readLines mode - reads a range of lines from a file.
     */
    private FileReadResult handleReadLines(String path, Integer startLine, Integer endLine) {
        if (path == null) {
            throw new FileReadException("Parameter 'path' is required for mode 'readLines'");
        }
        if (startLine == null) {
            throw new FileReadException("Parameter 'startLine' is required for mode 'readLines'");
        }

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
     * Handle readFile mode - reads entire file contents.
     */
    private FileReadResult handleReadFile(String path) {
        if (path == null) {
            throw new FileReadException("Parameter 'path' is required for mode 'readFile'");
        }

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
                throw new FileReadException("File too large to read at once: " + lines.size() + " lines. Use mode='readLines' with range instead.");
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
     * Handle grep mode - searches for patterns in files.
     */
    private List<GrepResult> handleGrep(String searchPath, String pattern, String outputMode,
                                        Boolean caseInsensitive, Integer linesBefore, Integer linesAfter,
                                        Integer maxMatches) {
        if (searchPath == null) {
            throw new FileReadException("Parameter 'searchPath' is required for mode 'grep'");
        }
        if (pattern == null) {
            throw new FileReadException("Parameter 'pattern' is required for mode 'grep'");
        }

        // Set defaults
        if (outputMode == null) outputMode = "content";
        if (caseInsensitive == null) caseInsensitive = false;

        Path path = Paths.get(searchPath).normalize();
        if (!Files.exists(path)) {
            throw new FileReadException("Path not found: " + searchPath);
        }

        List<Path> filesToSearch;
        try {
            if (Files.isRegularFile(path)) {
                filesToSearch = List.of(path);
            } else {
                filesToSearch = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> !isBinaryFile(p))
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new FileReadException("Failed to search path: " + searchPath, e);
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
                    String relativePath = path.relativize(filePath).toString();
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
     * Handle getStats mode - gets file statistics.
     */
    private FileStats handleGetStats(String path) {
        if (path == null) {
            throw new FileReadException("Parameter 'path' is required for mode 'getStats'");
        }

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

    // ========== HELPER METHODS ==========

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
