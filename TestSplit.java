public class TestSplit {
    public static void main(String[] args) {
        String content = "Line 1\nLine 3";
        java.util.List<String> lines = java.util.Arrays.asList(content.split("\n"));
        System.out.println("Lines: " + lines.size());
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(i + ": '" + lines.get(i) + "'");
        }
    }
}