import java.util.*;

public class TestMemory {
    public static void main(String[] args) {
        // Simulate what we're getting
        List<String> lines = new ArrayList<>(Arrays.asList("Line 2", "Line 1", "Line 3"));
        System.out.println("Lines: " + lines.size());
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(i + ": '" + lines.get(i) + "'");
        }
        
        // What should we get for lines.get(2)?
        System.out.println("Line 2: '" + lines.get(2) + "'");
    }
}