package com.sspku.agent.module.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.embedding.tongyi")
public class EmbeddingConfig {
    private String apiKey;
    private String model = "text-embedding-v1";
    private int dimension = 1536;
    private int batchSize = 10;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
}
