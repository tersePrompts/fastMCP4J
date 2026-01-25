package io.github.fastmcp.handler;

import io.github.fastmcp.model.*;
import io.github.fastmcp.notification.NotificationSender;
import io.github.fastmcp.notification.ProgressTracker;
import io.github.fastmcp.util.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ListResourcesHandler {
    private static final Logger log = LoggerFactory.getLogger(ListResourcesHandler.class);
    
    private final ServerMeta meta;
    private final NotificationSender notificationSender;
    private final ProgressTracker progressTracker;
    private final PaginationHelper paginationHelper;
    
    public ListResourcesHandler(ServerMeta meta, NotificationSender notificationSender) {
        this.meta = meta;
        this.notificationSender = notificationSender;
        this.progressTracker = notificationSender != null ? new ProgressTracker(notificationSender) : null;
        this.paginationHelper = progressTracker != null ? new PaginationHelper(progressTracker) : new PaginationHelper(null);
    }
    
    public BiFunction<Object, Object, Mono<ListResourcesResult>> asHandler() {
        return (exchange, request) -> {
            try {
                String cursor = extractCursor(request);
                String progressToken = extractProgressToken(request);
                
                List<ResourceMeta> allResources = meta.getResources();
                
                if (progressTracker != null && progressToken != null) {
                    progressTracker.track(progressToken);
                }
                
                PaginatedResult<ResourceMeta> paginated = paginationHelper.paginate(allResources, cursor);
                
                if (progressTracker != null && progressToken != null) {
                    double progress = paginated.hasMore() ? 50.0 : 100.0;
                    progressTracker.update(progressToken, progress, "Retrieved " + paginated.items().size() + " resources");
                    
                    if (!paginated.hasMore()) {
                        progressTracker.complete(progressToken);
                    }
                }
                
                ListResourcesResult result = ListResourcesResult.builder()
                    .resources(paginated.items())
                    .nextCursor(paginated.nextCursor())
                    .build();
                
                return Mono.just(result);
            } catch (Exception e) {
                log.error("Error in ListResourcesHandler", e);
                return Mono.just(errorResult(e));
            }
        };
    }
    
    private String extractCursor(Object request) {
        if (request instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) request;
            Object cursor = map.get("cursor");
            return cursor != null ? cursor.toString() : null;
        }
        return null;
    }
    
    private String extractProgressToken(Object request) {
        if (request instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) request;
            Object meta = map.get("_meta");
            if (meta instanceof Map) {
                Map<?, ?> metaMap = (Map<?, ?>) meta;
                Object token = metaMap.get("progressToken");
                return token != null ? token.toString() : null;
            }
        }
        return null;
    }
    
    private ListResourcesResult errorResult(Exception e) {
        return ListResourcesResult.builder()
            .resources(List.of())
            .nextCursor(null)
            .build();
    }
}
