package io.github.fastmcp.examples;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.core.FastMCP;
import io.github.fastmcp.notification.ProgressContext;
import reactor.core.publisher.Mono;

@McpServer(name = "PaginationExample", version = "1.0.0", instructions = "Example server demonstrating pagination and progress tracking")
public class PaginationExample {
    
    @McpTool(description = "Simple tool without progress tracking")
    public String simpleEcho(String message) {
        return "Echo: " + message;
    }
    
    @McpTool(description = "Tool with progress tracking")
    @WithProgress(description = "Process items with progress updates")
    public String processWithProgress(int total, ProgressContext progress) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 1; i <= total; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            progress.report(i, total, "Processing step " + i + " of " + total);
            result.append("Step ").append(i).append(" processed\n");
        }
        
        return result.toString();
    }
    
    @McpTool(description = "Async tool with progress tracking")
    @WithProgress
    @McpAsync
    public Mono<String> asyncProcessWithProgress(int total, ProgressContext progress) {
        return Mono.fromCallable(() -> {
            StringBuilder result = new StringBuilder();
            
            for (int i = 1; i <= total; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                progress.report(i, total, "Async step " + i + " of " + total);
                result.append("Async step ").append(i).append(" processed\n");
            }
            
            return result.toString();
        });
    }
    
    public static void main(String[] args) {
        FastMCP.server(PaginationExample.class)
            .withNotificationSender(message -> {
                System.out.println("NOTIFICATION: " + message);
            })
            .stdio()
            .run();
    }
}
