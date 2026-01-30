package com.ultrathink.fastmcp.agent.tenancy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for tenant resolvers. Searches resolvers in priority order.
 */
@Slf4j
public final class TenantResolverRegistry {

    private final List<TenantResolver> resolvers = new CopyOnWriteArrayList<>();

    public TenantResolverRegistry() {
        // Add default resolver last (lowest priority)
        addResolver(new DefaultTenantResolver());
    }

    /**
     * Register a tenant resolver
     */
    public void addResolver(TenantResolver resolver) {
        resolvers.add(resolver);
        // Sort by priority (descending)
        resolvers.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    }

    /**
     * Resolve tenant from the current context
     */
    public Optional<TenantContext> resolve(Map<String, String> headers, String sessionId) {
        for (TenantResolver resolver : resolvers) {
            if (!resolver.isEnabled()) continue;
            try {
                Optional<TenantContext> result = resolver.resolve(headers, sessionId);
                if (result.isPresent()) {
                    log.debug("Resolved tenant {} using resolver {}", result.get().getTenantId(), resolver.getClass().getSimpleName());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Tenant resolver {} failed", resolver.getClass().getSimpleName(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolve tenant or return default
     */
    public TenantContext resolveOrDefault(Map<String, String> headers, String sessionId) {
        return resolve(headers, sessionId).orElseGet(TenantContext::defaultContext);
    }

    /**
     * Default tenant resolver - returns default context
     */
    private static class DefaultTenantResolver implements TenantResolver {
        @Override
        public Optional<TenantContext> resolve(Map<String, String> headers, String sessionId) {
            return Optional.of(TenantContext.defaultContext());
        }

        @Override
        public int priority() {
            return Integer.MIN_VALUE; // Always last
        }
    }
}
