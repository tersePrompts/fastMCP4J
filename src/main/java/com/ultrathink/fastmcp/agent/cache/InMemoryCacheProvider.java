package com.ultrathink.fastmcp.agent.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory cache provider with TTL support.
 * Default implementation of CacheProvider SPI.
 */
@Slf4j
public class InMemoryCacheProvider implements CacheProvider, AutoCloseable {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private CacheStats stats = CacheStats.empty();

    private final long maxEntries;
    private final Duration defaultTtl;

    public InMemoryCacheProvider() {
        this(1000, Duration.ofMinutes(5));
    }

    public InMemoryCacheProvider(long maxEntries, Duration defaultTtl) {
        this.maxEntries = maxEntries;
        this.defaultTtl = defaultTtl;
        startCleanupTask();
    }

    @Override
    public Optional<CacheEntry> get(CacheKey key) {
        CacheEntry entry = cache.get(key.toString());
        if (entry == null) {
            stats = stats.incrementMisses();
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.remove(key.toString());
            stats = stats.incrementMisses();
            return Optional.empty();
        }
        stats = stats.incrementHits();
        return Optional.of(entry);
    }

    @Override
    public void put(CacheKey key, CacheEntry entry) {
        // Enforce max entries
        if (cache.size() >= maxEntries) {
            evictOldest();
        }
        cache.put(key.toString(), entry);
    }

    @Override
    public void invalidate(CacheKey key) {
        cache.remove(key.toString());
    }

    @Override
    public void evictSession(String sessionId) {
        cache.keySet().removeIf(key -> {
            // Keys are in format: /tenant/session/tool/key
            String[] parts = key.split("/", 5);
            return parts.length >= 3 && parts[2].equals(sessionId);
        });
    }

    @Override
    public void evictTenant(String tenantId) {
        cache.keySet().removeIf(key -> key.startsWith("/" + tenantId + "/"));
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public CacheStats getStats() {
        return stats;
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clear();
    }

    private void evictOldest() {
        // Simple strategy: remove first entry
        cache.keySet().stream().findFirst().ifPresent(cache::remove);
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Instant now = Instant.now();
                cache.entrySet().removeIf(entry ->
                    entry.getValue().isExpired() || now.isAfter(entry.getValue().expiresAt())
                );
            } catch (Exception e) {
                log.warn("Cache cleanup error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Create a CacheKey for caching tool results
     */
    public static CacheKey forTool(String toolName, Map<String, Object> args) {
        String key = toolName + ":" + args.hashCode();
        return CacheKey.of(toolName, key);
    }

    /**
     * Create a CacheKey for session-scoped caching
     */
    public static CacheKey forSession(String sessionId, String toolName, Map<String, Object> args) {
        String key = toolName + ":" + args.hashCode();
        return new CacheKey("default", sessionId, toolName, key);
    }
}
