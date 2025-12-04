package com.sspku.agent.module.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.vector-store.milvus")
public class VectorStorageConfig {

    private String host = "localhost";
    private int port = 19530;
    private String token; // For authenticated Milvus

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port);
        
        if (token != null && !token.isEmpty()) {
            builder.withToken(token);
        }
        
        return new MilvusServiceClient(builder.build());
    }
}
