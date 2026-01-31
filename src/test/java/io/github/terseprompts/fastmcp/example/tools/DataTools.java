package io.github.terseprompts.fastmcp.example.tools;

import io.github.terseprompts.fastmcp.annotations.McpResource;
import io.github.terseprompts.fastmcp.annotations.McpTool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Data processing and retrieval tools.
 * These will be auto-discovered by package scanning.
 */
public class DataTools {

    @McpTool(description = "Generate a random number between min and max")
    public int randomNumber(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        return min + (int)(Math.random() * (max - min + 1));
    }

    @McpTool(description = "Get current date and time")
    public String currentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @McpTool(description = "Calculate hash of a string")
    public String hashString(String input) {
        return String.valueOf(input.hashCode());
    }

    @McpTool(description = "Validate email format")
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @McpResource(uri = "data://sample", name = "Sample Data", mimeType = "application/json")
    public String getSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("status", "active");
        data.put("count", 42);
        data.put("items", new String[]{"item1", "item2", "item3"});

        StringBuilder sb = new StringBuilder("{\n");
        data.forEach((key, value) -> {
            if (value instanceof String[]) {
                sb.append("  \"").append(key).append("\": [");
                String[] arr = (String[]) value;
                for (int i = 0; i < arr.length; i++) {
                    sb.append("\"").append(arr[i]).append("\"");
                    if (i < arr.length - 1) sb.append(", ");
                }
                sb.append("],\n");
            } else {
                sb.append("  \"").append(key).append("\": \"").append(value).append("\",\n");
            }
        });
        sb.append("}");
        return sb.toString();
    }
}
