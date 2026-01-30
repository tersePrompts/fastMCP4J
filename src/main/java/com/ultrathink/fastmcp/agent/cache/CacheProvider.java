package com.ultrathink.fastmcp.agent.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * SPI for cache providers.
 * Implementations can provide in-memory, Redis, or other caching strategies.
 */
public interface CacheProvider {

    /**
     * Get a cached value
     * @param key Cache key
     * @return Cached entry, or empty if not found or expired
     */
    Optional<CacheEntry> get(CacheKey key);

    /**
     * Put a value in cache with TTL
     * @param key Cache key
     * @param entry Cache entry
     */
    void put(CacheKey key, CacheEntry entry);

    /**
     * Invalidate a specific cache entry
     * @param key Cache key to invalidate
     */
    void invalidate(CacheKey key);

    /**
     * Invalidate all entries for a session
     * @param sessionId Session ID
     */
    void evictSession(String sessionId);

    /**
     * Invalidate all entries for a tenant
     * @param tenantId Tenant ID
     */
    void evictTenant(String tenantId);

    /**
     * Clear all cache entries
     */
    void clear();

    /**
     * Get cache statistics
     * @return Cache stats
     */
    CacheStats getStats();

    /** Cache key with tenant/session scoping */
    record CacheKey(
        String tenantId,
        String sessionId,
        String toolName,
        String key
    ) {
        public static CacheKey of(String toolName, String key) {
            return new CacheKey("default", null, toolName, key);
        }

        public static CacheKey of(String sessionId, String toolName, String key) {
            return new CacheKey("default", sessionId, toolName, key);
        }

        public String toString() {
            return String.format("/%s/%s/%s/%s", tenantId,
                sessionId != null ? sessionId : "*", toolName, key);
        }
    }

    /** Cache entry with value and expiry */
    record CacheEntry(
        Object value,
        java.time.Instant expiresAt
    ) {
        public CacheEntry(Object value, Duration ttl) {
            this(value, java.time.Instant.now().plus(ttl));
        }

        public boolean isExpired() {
            return java.time.Instant.now().isAfter(expiresAt);
        }
    }

    /** Cache statistics */
    record CacheStats(
        long totalEntries,
        long hitCount,
        long missCount,
        double hitRate
    ) {
        public static CacheStats empty() {
            return new CacheStats(0, 0, 0, 0.0);
        }

        public CacheStats incrementHits() {
            return new CacheStats(totalEntries, hitCount + 1, missCount, hitRate());
        }

        public CacheStats incrementMisses() {
            return new CacheStats(totalEntries, hitCount, missCount + 1, hitRate());
        }

        private double hitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
    }
}
