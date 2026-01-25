import java.util.*;

public class TestInsert {
    public static void main(String[] args) {
        // Simulate original content: "Line 1\nLine 3"
        List<String> lines = new ArrayList<>(Arrays.asList("Line 1", "Line 3"));
        
        // Insert at line 1: "Line 2\n"
        int insertLine = 1;
        String insertText = "Line 2\n";
        
        lines.add(insertLine, insertText);
        String newContent = String.join("\n", lines);
        
        System.out.println("Result lines: " + lines.size());
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(i + ": '" + lines.get(i) + "'");
        }
    }
}