package io.github.fastmcp.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ProgressNotification {
    @JsonProperty("progressToken")
    private final Object progressToken;
    
    @JsonProperty("progress")
    private final double progress;
    
    @JsonProperty("total")
    private final Double total;
    
    @JsonProperty("message")
    private final String message;
    
    public ProgressNotification(double progressToken, double progress, String message) {
        this(progressToken, progress, null, message);
    }
    
    public ProgressNotification(Object progressToken, double progress, Double total, String message) {
        this.progressToken = progressToken;
        this.progress = progress;
        this.total = total;
        this.message = message;
    }
    
    public Object progressToken() { return progressToken; }
    public double progress() { return progress; }
    public Double total() { return total; }
    public String message() { return message; }
    
    public static ProgressNotification of(String token, double progress, String message) {
        return new ProgressNotification(token, progress, null, message);
    }
    
    public static ProgressNotification of(String token, double progress, double total, String message) {
        return new ProgressNotification(token, progress, total, message);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressNotification that = (ProgressNotification) o;
        return Double.compare(progress, that.progress) == 0 && Objects.equals(progressToken, that.progressToken);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(progressToken, progress);
    }
}
