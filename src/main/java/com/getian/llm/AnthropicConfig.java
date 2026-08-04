package com.getian.llm;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnthropicConfig {
    private static final int  MAX_TOKENS = 6000;
    private String baseUrl;
    private String model;
    private String apiKey;
    private String systemPrompt;
    private int maxTokens;
    public AnthropicConfig(String baseUrl,String model,String apiKey,String systemPrompt){
        this(baseUrl,model,apiKey,systemPrompt,MAX_TOKENS);
    }

    public AnthropicConfig(String baseUrl,String model,String apiKey,String systemPrompt,int maxTokens){
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.systemPrompt = systemPrompt;
        this.maxTokens = maxTokens;
    }
}
