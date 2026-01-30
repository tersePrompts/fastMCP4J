package com.ultrathink.fastmcp.agent.tenancy;

import java.util.Map;
import java.util.Optional;

/**
 * SPI for resolving tenant information from incoming requests.
 */
public interface TenantResolver {

    /**
     * Resolve tenant from the current request context
     * @param headers HTTP headers or transport metadata
     * @param sessionId Current session ID
     * @return TenantContext or empty if no tenant resolved
     */
    Optional<TenantContext> resolve(Map<String, String> headers, String sessionId);

    /**
     * Priority for resolver ordering (higher = earlier)
     */
    default int priority() {
        return 0;
    }

    /**
     * Whether this resolver is enabled
     */
    default boolean isEnabled() {
        return true;
    }
}
