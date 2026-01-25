package io.github.fastmcp.adapter;

import io.github.fastmcp.notification.ProgressContext;
import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.notification.NotificationSender;
import io.github.fastmcp.notification.ProgressTracker;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import io.github.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.sdk.CallToolRequest;
import io.modelcontextprotocol.sdk.CallToolResult;
import io.modelcontextprotocol.sdk.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Collections;

public class ProgressAwareToolHandler {
    private static final Logger log = LoggerFactory.getLogger(ProgressAwareToolHandler.class);
    
    private final Object instance;
    private final ToolMeta meta;
    private final ArgumentBinder binder;
    private final ResponseMarshaller marshaller;
    private final NotificationSender notificationSender;
    private final ProgressTracker progressTracker;
    
    public ProgressAwareToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder, 
                                     ResponseMarshaller marshaller, NotificationSender notificationSender) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
        this.notificationSender = notificationSender;
        this.progressTracker = notificationSender != null ? new ProgressTracker(notificationSender) : null;
    }
    
    public java.util.function.BiFunction<Object, CallToolRequest, Mono<CallToolResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                
                if (meta.isProgressEnabled() && progressTracker != null) {
                    String progressToken = extractProgressToken(request.arguments());
                    progressTracker.track(progressToken);
                    
                    Object result = invokeWithProgress(args, progressToken);
                    
                    if (meta.isAsync()) {
                        return ((Mono<?>) result).map(marshaller::marshal)
                            .doOnSuccess(r -> progressTracker.complete(progressToken))
                            .doOnError(e -> progressTracker.fail(progressToken, e.getMessage()));
                    } else {
                        progressTracker.complete(progressToken);
                        return Mono.just(marshaller.marshal(result));
                    }
                } else {
                    Object result = meta.getMethod().invoke(instance, args);
                    
                    if (meta.isAsync()) {
                        return ((Mono<?>) result).map(marshaller::marshal);
                    } else {
                        return Mono.just(marshaller.marshal(result));
                    }
                }
            } catch (Exception e) {
                log.error("Error in ProgressAwareToolHandler", e);
                return Mono.just(errorResult(e));
            }
        };
    }
    
    private Object invokeWithProgress(Object[] args, String progressToken) throws Exception {
        Method method = meta.getMethod();
        Class<?>[] paramTypes = method.getParameterTypes();
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == ProgressContext.class) {
                ProgressContext ctx = new ProgressContext(progressToken, 0.0, null, "");
                args[i] = ctx;
                break;
            }
        }
        
        return method.invoke(instance, args);
    }
    
    private String extractProgressToken(java.util.Map<String, Object> arguments) {
        if (arguments == null) {
            return "progress_" + System.currentTimeMillis();
        }
        Object token = arguments.get("progressToken");
        return token != null ? token.toString() : "progress_" + System.currentTimeMillis();
    }
    
    private CallToolResult errorResult(Exception e) {
        return CallToolResult.builder()
            .content(Collections.singletonList(new TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
