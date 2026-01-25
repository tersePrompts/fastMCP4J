package com.ultrathink.fastmcp.sampling;

import java.util.List;

/**
 * Request for LLM sampling/completion.
 *
 * @version 0.2.0
 * @status NOT_IMPLEMENTED
 */
public record SamplingRequest(
    String prompt,
    Integer maxTokens,
    Double temperature,
    Double topP,
    List<String> stopSequences,
    String modelPreference
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prompt;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private List<String> stopSequences;
        private String modelPreference;

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(double topP) {
            this.topP = topP;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public Builder modelPreference(String modelPreference) {
            this.modelPreference = modelPreference;
            return this;
        }

        public SamplingRequest build() {
            return new SamplingRequest(
                prompt,
                maxTokens,
                temperature,
                topP,
                stopSequences,
                modelPreference
            );
        }
    }
}
