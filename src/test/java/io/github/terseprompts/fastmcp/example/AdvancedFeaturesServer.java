package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.context.McpContext;
import io.github.terseprompts.fastmcp.annotations.McpInject;
import io.github.terseprompts.fastmcp.annotations.McpOnShutdown;
import io.github.terseprompts.fastmcp.annotations.McpOnStartup;
import io.github.terseprompts.fastmcp.annotations.McpParam;
import io.github.terseprompts.fastmcp.annotations.McpResource;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.annotations.McpTool;
import io.github.terseprompts.fastmcp.context.Context;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Demo server exercising the advanced capabilities of FastMCP4J end-to-end
 * from a real Python MCP client over Streamable HTTP.
 *
 * <p>Exercised features:
 * <ol>
 *   <li>Protocol notifications (progress + logging) via {@code Context}.</li>
 *   <li>Resource templates ({@code user://profile/{name}}).</li>
 *   <li>LLM sampling ({@code ctx.sample(prompt)}).</li>
 *   <li>DI + lifespan ({@code @McpInject}, {@code @McpOnStartup}, {@code @McpOnShutdown}).</li>
 *   <li>Middleware wrapping tool invocations.</li>
 * </ol>
 *
 * <p>Runs on Streamable HTTP endpoint {@code /mcp} at port 3003.
 */
@McpServer(name = "advanced-features", version = "1.0.0")
public class AdvancedFeaturesServer {

    /** Simple bean registered by type to prove type-based DI. */
    public static class SomeBean {
        private final String value;

        public SomeBean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /** Injected by name from {@code .inject("configValue", ...)}. */
    @McpInject
    private String configValue;

    /** Injected by type from {@code .inject(SomeBean.class, ...)}. */
    @McpInject
    private SomeBean someBean;

    /** Set to true once {@code @McpOnStartup} has run. */
    private boolean startupRan = false;

    /** Set to true once {@code @McpOnShutdown} has run. */
    private boolean shutdownRan = false;

    @McpOnStartup
    public void onStartup() {
        startupRan = true;
    }

    @McpOnShutdown
    public void onShutdown() {
        shutdownRan = true;
    }

    @McpTool(description = "Sample an LLM completion from the connected client")
    public String sampledTool(
            @McpParam(description = "Prompt to send to the model") String prompt,
            @McpContext Context ctx) {
        return ctx.sample(prompt);
    }

    @McpTool(description = "Emit progress and logging notifications, then return a value")
    public String progressEcho(
            @McpParam(description = "Message to echo") String message,
            @McpContext Context ctx) {
        ctx.info("processing " + message);
        ctx.reportProgress(0, 100, "start");
        ctx.debug("about to finish " + message);
        ctx.reportProgress(100, 100, "done");
        return "done:" + message;
    }

    @McpTool(description = "Echo a message (wrapped by middleware)")
    public String echo(@McpParam(description = "Message to echo") String message) {
        return "ECHO: " + message;
    }

    @McpTool(description = "Return the value injected via @McpInject (by name)")
    public String getInjectedValue() {
        return configValue != null ? configValue : "null";
    }

    @McpTool(description = "Return the bean injected via @McpInject (by type)")
    public String getInjectedBeanValue() {
        return someBean != null ? someBean.getValue() : "null";
    }

    @McpTool(description = "Return whether @McpOnStartup ran")
    public String startupFlag() {
        return startupRan ? "true" : "false";
    }

    @McpResource(uri = "server://info")
    public String info() {
        return "server-info";
    }

    @McpResource(uri = "user://profile/{name}")
    public String profile(@McpParam(description = "user name") String name) {
        return "Profile for: " + name;
    }

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("  Advanced Features Server");
        System.out.println("  Streamable HTTP on port 3003 (/mcp)");
        System.out.println("=============================================");

        FastMCP.server(AdvancedFeaturesServer.class)
                .streamable()
                .port(3003)
                .middleware((toolName, arguments, next) -> {
                    if ("echo".equals(toolName)) {
                        Object raw = next.invoke(toolName, arguments);
                        if (raw instanceof String s) {
                            return s + " [mw]";
                        }
                        return raw;
                    }
                    return next.invoke(toolName, arguments);
                })
                .inject("configValue", "injected-secret-123")
                .inject(SomeBean.class, new SomeBean("bean-value-456"))
                .run();
    }
}
