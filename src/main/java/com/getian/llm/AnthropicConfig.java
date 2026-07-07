package com.getian.llm;

import lombok.Data;

@Data
public class AnthropicConfig {
    private static final int  MAX_TOKENS = 6000;
    private String baseUrl;
    private String model;
    private String apiKey;
    private String systemPrompt;
    private int maxTokens = MAX_TOKENS;
}
