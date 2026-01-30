package com.ultrathink.fastmcp.agent.tenancy;

import java.util.Map;

/**
 * Tenant context containing tenant, user, and namespace information.
 */
public final class TenantContext {

    private final String tenantId;
    private final String userId;
    private final String namespace;

    private TenantContext(String tenantId, String userId, String namespace) {
        this.tenantId = tenantId != null ? tenantId : "default";
        this.userId = userId;
        this.namespace = namespace != null ? namespace : "default";
    }

    public static TenantContext of(String tenantId, String userId, String namespace) {
        return new TenantContext(tenantId, userId, namespace);
    }

    public static TenantContext of(String tenantId) {
        return new TenantContext(tenantId, null, "default");
    }

    public static TenantContext defaultContext() {
        return new TenantContext("default", null, "default");
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the full path prefix for tenant-isolated resources
     * Format: /{tenant}/{user}/{namespace}/
     */
    public String getPathPrefix() {
        StringBuilder sb = new StringBuilder("/").append(tenantId);
        if (userId != null) {
            sb.append("/").append(userId);
        }
        sb.append("/").append(namespace);
        return sb.toString();
    }

    /**
     * Resolve a path within the tenant context
     */
    public String resolvePath(String relativePath) {
        String basePath = getPathPrefix();
        String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return basePath + "/" + normalized;
    }
}
