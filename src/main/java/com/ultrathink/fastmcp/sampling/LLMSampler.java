package com.ultrathink.fastmcp.sampling;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for requesting LLM completions from MCP clients.
 * Enables servers to request AI assistance during tool execution.
 *
 * @version 0.2.0
 * @status NOT_IMPLEMENTED
 */
public interface LLMSampler {

    /**
     * Request a synchronous completion from the client's LLM
     * @param request sampling request with prompt and parameters
     * @return completion text
     */
    String complete(SamplingRequest request);

    /**
     * Request an asynchronous completion
     * @param request sampling request
     * @return Mono emitting completion text
     */
    Mono<String> completeAsync(SamplingRequest request);

    /**
     * Request a streaming completion
     * @param request sampling request
     * @return Flux streaming completion chunks
     */
    Flux<String> completeStream(SamplingRequest request);
}
