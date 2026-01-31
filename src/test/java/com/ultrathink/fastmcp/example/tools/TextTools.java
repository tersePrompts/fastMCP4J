package com.ultrathink.fastmcp.example.tools;

import com.ultrathink.fastmcp.annotations.McpTool;

/**
 * Text processing tools.
 * These will be auto-discovered by package scanning.
 */
public class TextTools {

    @McpTool(description = "Count characters in a string (excluding spaces)")
    public int charCount(String text) {
        return text.replace(" ", "").length();
    }

    @McpTool(description = "Extract numbers from a string")
    public String extractNumbers(String text) {
        return text.replaceAll("[^0-9]", " ").trim().replaceAll("\\s+", ", ");
    }

    @McpTool(description = "Check if string contains a substring")
    public boolean contains(String text, String substring) {
        return text.contains(substring);
    }

    @McpTool(description = "Replace all occurrences of a substring")
    public String replaceAll(String text, String target, String replacement) {
        return text.replace(target, replacement);
    }

    @McpTool(description = "Split string by delimiter")
    public String[] split(String text, String delimiter) {
        return text.split(delimiter);
    }

    @McpTool(description = "Trim whitespace from string")
    public String trim(String text) {
        return text.trim();
    }

    @McpTool(description = "Convert string to title case")
    public String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
