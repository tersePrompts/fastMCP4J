package io.github.fastmcp.handler;

import io.github.fastmcp.model.*;
import io.github.fastmcp.notification.NotificationSender;
import io.github.fastmcp.notification.ProgressTracker;
import io.github.fastmcp.util.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ListToolsHandler {
    private static final Logger log = LoggerFactory.getLogger(ListToolsHandler.class);
    
    private final ServerMeta meta;
    private final NotificationSender notificationSender;
    private final ProgressTracker progressTracker;
    private final PaginationHelper paginationHelper;
    
    public ListToolsHandler(ServerMeta meta, NotificationSender notificationSender) {
        this.meta = meta;
        this.notificationSender = notificationSender;
        this.progressTracker = notificationSender != null ? new ProgressTracker(notificationSender) : null;
        this.paginationHelper = progressTracker != null ? new PaginationHelper(progressTracker) : new PaginationHelper(null);
    }
    
    public BiFunction<Object, Object, Mono<ListToolsResult>> asHandler() {
        return (exchange, request) -> {
            try {
                String cursor = extractCursor(request);
                String progressToken = extractProgressToken(request);
                
                List<ToolMeta> allTools = meta.getTools();
                
                if (progressTracker != null && progressToken != null) {
                    progressTracker.track(progressToken);
                }
                
                PaginatedResult<ToolMeta> paginated = paginationHelper.paginate(allTools, cursor);
                
                if (progressTracker != null && progressToken != null) {
                    double progress = paginated.hasMore() ? 50.0 : 100.0;
                    progressTracker.update(progressToken, progress, "Retrieved " + paginated.items().size() + " tools");
                    
                    if (!paginated.hasMore()) {
                        progressTracker.complete(progressToken);
                    }
                }
                
                ListToolsResult result = ListToolsResult.builder()
                    .tools(paginated.items())
                    .nextCursor(paginated.nextCursor())
                    .build();
                
                return Mono.just(result);
            } catch (Exception e) {
                log.error("Error in ListToolsHandler", e);
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
    
    private ListToolsResult errorResult(Exception e) {
        return ListToolsResult.builder()
            .tools(List.of())
            .nextCursor(null)
            .build();
    }
}
