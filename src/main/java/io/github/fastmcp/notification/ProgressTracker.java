package io.github.fastmcp.notification;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ProgressTracker {
    private final NotificationSender sender;
    private final ConcurrentMap<String, ActiveProgress> activeProgress = new ConcurrentHashMap<>();
    
    private static final long MIN_NOTIFY_INTERVAL_MS = 100;
    
    public ProgressTracker(NotificationSender sender) {
        this.sender = sender;
    }
    
    public void track(String progressToken) {
        activeProgress.put(progressToken, new ActiveProgress(progressToken));
    }
    
    public void update(String progressToken, double progress, String message) {
        update(progressToken, progress, null, message);
    }
    
    public void update(String progressToken, double progress, Double total, String message) {
        ActiveProgress ap = activeProgress.get(progressToken);
        if (ap == null) return;
        
        if (!ap.shouldNotify(progress)) return;
        
        sender.progress(Double.parseDouble(progressToken), progress, message);
        ap.lastUpdate.set(System.currentTimeMillis());
        ap.lastProgress.set(progress);
    }
    
    public void complete(String progressToken) {
        complete(progressToken, "Completed");
    }
    
    public void complete(String progressToken, String message) {
        ActiveProgress ap = activeProgress.remove(progressToken);
        if (ap == null) return;
        
        sender.progress(Double.parseDouble(ap.token), 100.0, message);
    }
    
    public void fail(String progressToken, String error) {
        ActiveProgress ap = activeProgress.remove(progressToken);
        if (ap == null) return;
        
        sender.progress(Double.parseDouble(ap.token), ap.lastProgress.get(), "Error: " + error);
    }
    
    public void remove(String progressToken) {
        activeProgress.remove(progressToken);
    }
    
    private static class ActiveProgress {
        final String token;
        final AtomicReference<Double> lastProgress = new AtomicReference<>(0.0);
        final AtomicReference<Long> lastUpdate = new AtomicReference<>(0L);
        final AtomicBoolean isComplete = new AtomicBoolean(false);
        
        ActiveProgress(String token) {
            this.token = token;
            this.lastUpdate.set(System.currentTimeMillis());
        }
        
        boolean shouldNotify(double newProgress) {
            if (isComplete.get()) return false;
            if (newProgress < lastProgress.get()) return false;
            
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdate.get();
            
            if (elapsed < MIN_NOTIFY_INTERVAL_MS && newProgress < 100) {
                return false;
            }
            
            if (newProgress <= lastProgress.get()) return false;
            
            return true;
        }
    }
}
