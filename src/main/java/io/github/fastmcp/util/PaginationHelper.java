package io.github.fastmcp.util;

import io.github.fastmcp.model.PageCursor;
import io.github.fastmcp.model.PaginatedResult;
import io.github.fastmcp.notification.ProgressContext;
import io.github.fastmcp.notification.ProgressTracker;

import java.util.List;
import java.util.function.Function;

public class PaginationHelper {
    private final ProgressTracker progressTracker;
    private final int defaultPageSize;
    
    public PaginationHelper(ProgressTracker progressTracker) {
        this(progressTracker, 50);
    }
    
    public PaginationHelper(ProgressTracker progressTracker, int defaultPageSize) {
        this.progressTracker = progressTracker;
        this.defaultPageSize = defaultPageSize;
    }
    
    public <T> PaginatedResult<T> paginate(List<T> allItems, String cursor, int pageSize) {
        PageCursor pageCursor = PageCursor.parse(cursor);
        
        int offset = pageCursor.offset();
        int limit = pageSize > 0 ? pageSize : defaultPageSize;
        
        int end = Math.min(offset + limit, allItems.size());
        
        List<T> pageItems = allItems.subList(offset, end);
        
        String nextCursor = null;
        if (end < allItems.size()) {
            nextCursor = pageCursor.next().encode();
        }
        
        return PaginatedResult.of(pageItems, nextCursor);
    }
    
    public <T> PaginatedResult<T> paginate(List<T> allItems, String cursor) {
        return paginate(allItems, cursor, defaultPageSize);
    }
    
    public <T> PaginatedResult<T> paginateAndTrack(
            List<T> allItems, 
            String cursor, 
            ProgressContext progressCtx,
            int pageSize
    ) {
        if (progressCtx != null && progressTracker != null) {
            progressTracker.track(progressCtx.progressToken());
        }
        
        PaginatedResult<T> result = paginate(allItems, cursor, pageSize);
        
        if (progressCtx != null && progressTracker != null) {
            progressTracker.update(
                progressCtx.progressToken(),
                result.items().size(),
                "Retrieved " + result.items().size() + " items"
            );
            
            if (!result.hasMore()) {
                progressTracker.complete(progressCtx.progressToken());
            }
        }
        
        return result;
    }
    
    public <T> PaginatedResult<T> paginateAndTrack(
            List<T> allItems, 
            String cursor, 
            ProgressContext progressCtx
    ) {
        return paginateAndTrack(allItems, cursor, progressCtx, defaultPageSize);
    }
    
    public <T, R> PaginatedResult<R> paginateStream(
            Function<String, PaginatedResult<R>> pageFetcher,
            String cursor,
            ProgressContext progressCtx
    ) {
        if (progressCtx != null && progressTracker != null) {
            progressTracker.track(progressCtx.progressToken());
        }
        
        PaginatedResult<R> result = pageFetcher.apply(cursor);
        
        if (progressCtx != null && progressTracker != null) {
            double progress = result.hasMore() ? 50.0 : 100.0;
            progressTracker.update(
                progressCtx.progressToken(),
                progress,
                "Retrieved " + result.items().size() + " items"
            );
            
            if (!result.hasMore()) {
                progressTracker.complete(progressCtx.progressToken());
            }
        }
        
        return result;
    }
}
