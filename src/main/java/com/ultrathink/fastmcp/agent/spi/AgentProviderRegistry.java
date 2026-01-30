package com.ultrathink.fastmcp.agent.spi;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for AgentProvider implementations.
 * Discovers providers via ServiceLoader and allows manual registration.
 */
@Slf4j
public final class AgentProviderRegistry {

    private static final Map<String, AgentProvider> providers = new ConcurrentHashMap<>();

    static {
        // Load providers via ServiceLoader
        try {
            ServiceLoader<AgentProvider> loader = ServiceLoader.load(AgentProvider.class);
            for (AgentProvider provider : loader) {
                register(provider);
                log.info("Discovered agent provider: {} v{}", provider.getName(), provider.getVersion());
            }
        } catch (Exception e) {
            log.debug("No agent providers found via ServiceLoader: {}", e.getMessage());
        }
    }

    private AgentProviderRegistry() {}

    /**
     * Register an agent provider
     */
    public static void register(AgentProvider provider) {
        providers.put(provider.getName(), provider);
        log.info("Registered agent provider: {}", provider.getName());
    }

    /**
     * Get a provider by name
     */
    public static AgentProvider getProvider(String name) {
        return providers.get(name);
    }

    /**
     * Get all registered providers
     */
    public static Map<String, AgentProvider> getAllProviders() {
        return Map.copyOf(providers);
    }

    /**
     * Initialize all providers with the given context
     */
    public static void initializeAll(AgentProviderContext context) {
        providers.values().forEach(p -> {
            try {
                p.initialize(context);
                log.info("Initialized agent provider: {}", p.getName());
            } catch (Exception e) {
                log.error("Failed to initialize provider: {}", p.getName(), e);
            }
        });
    }

    /**
     * Notify all providers that server is starting
     */
    public static void notifyServerStart() {
        providers.values().forEach(p -> {
            try {
                p.onServerStart();
            } catch (Exception e) {
                log.error("Error in onServerStart for provider: {}", p.getName(), e);
            }
        });
    }

    /**
     * Notify all providers that server is stopping
     */
    public static void notifyServerStop() {
        providers.values().forEach(p -> {
            try {
                p.onServerStop();
            } catch (Exception e) {
                log.error("Error in onServerStop for provider: {}", p.getName(), e);
            }
        });
    }

    /**
     * Notify all providers that a session is starting
     */
    public static void notifySessionStart(String sessionId) {
        providers.values().forEach(p -> {
            try {
                p.onSessionStart(sessionId);
            } catch (Exception e) {
                log.error("Error in onSessionStart for provider: {}", p.getName(), e);
            }
        });
    }

    /**
     * Notify all providers that a session is ending
     */
    public static void notifySessionEnd(String sessionId) {
        providers.values().forEach(p -> {
            try {
                p.onSessionEnd(sessionId);
            } catch (Exception e) {
                log.error("Error in onSessionEnd for provider: {}", p.getName(), e);
            }
        });
    }

    /**
     * Cleanup all providers
     */
    public static void cleanupAll() {
        providers.values().forEach(p -> {
            try {
                p.cleanup();
            } catch (Exception e) {
                log.error("Error in cleanup for provider: {}", p.getName(), e);
            }
        });
    }
}
