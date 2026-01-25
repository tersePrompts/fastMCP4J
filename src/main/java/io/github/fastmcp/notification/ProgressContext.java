package io.github.fastmcp.notification;

import java.util.Objects;

public class ProgressContext {
    private final String progressToken;
    private final double currentProgress;
    private final Double total;
    private final String message;
    
    public ProgressContext(String progressToken, double currentProgress, Double total, String message) {
        this.progressToken = progressToken;
        this.currentProgress = currentProgress;
        this.total = total;
        this.message = message;
    }
    
    public String progressToken() { return progressToken; }
    public double currentProgress() { return currentProgress; }
    public Double total() { return total; }
    public String message() { return message; }
    
    public static ProgressContext of(String token, double progress, String message) {
        return new ProgressContext(token, progress, null, message);
    }
    
    public static ProgressContext of(String token, double progress, double total, String message) {
        return new ProgressContext(token, progress, total, message);
    }
    
    public double getPercentage() {
        if (total == null || total == 0) return 0;
        return (currentProgress / total) * 100;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressContext that = (ProgressContext) o;
        return Objects.equals(progressToken, that.progressToken);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(progressToken);
    }
    
    @Override
    public String toString() {
        return "ProgressContext[token=" + progressToken + ", progress=" + currentProgress + "]";
    }
    
    public void report(double current) {
        this.report(current, current, "");
    }
    
    public void report(double current, double total, String message) {
        double percentage = (total > 0) ? (current / total) * 100 : 0;
        System.out.println("Progress: " + percentage + "% - " + (message.isEmpty() ? "Processing..." : message));
    }
}
