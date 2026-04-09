package com.cooking.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(
                500, // Max tokens per chunk.
                100, // Overlap in tokens between chunks.
                5,   // Min chunk size in characters.
                100000, // Max chunk size in characters.
                true   // Keep the separator.
        );
    }
}
