package com.sspku.agent.module.knowledge.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sspku.agent.module.knowledge.config.EmbeddingConfig;
import com.sspku.agent.module.knowledge.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TongyiEmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingConfig embeddingConfig;
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    @Override
    public List<List<Float>> embedDocuments(List<String> texts) {
        if (CollUtil.isEmpty(texts)) {
            return Collections.emptyList();
        }

        List<List<Float>> allEmbeddings = new ArrayList<>();
        int batchSize = embeddingConfig.getBatchSize();

        // Batch processing
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(texts.size(), i + batchSize);
            List<String> batch = texts.subList(i, end);
            
            try {
                List<List<Float>> batchEmbeddings = getEmbeddingsWithRetry(batch);
                allEmbeddings.addAll(batchEmbeddings);
                
                // Rate limiting delay
                if (end < texts.size()) {
                    TimeUnit.MILLISECONDS.sleep(200); 
                }
            } catch (Exception e) {
                log.error("Failed to embed batch {} to {}", i, end, e);
                throw new RuntimeException("Embedding failed", e);
            }
        }

        return allEmbeddings;
    }

    @Override
    public List<Float> embedQuery(String text) {
        return embedDocuments(Collections.singletonList(text)).get(0);
    }

    private List<List<Float>> getEmbeddingsWithRetry(List<String> texts) {
        int retries = 0;
        while (retries < embeddingConfig.getMaxRetries()) {
            try {
                return callApi(texts);
            } catch (Exception e) {
                retries++;
                log.warn("Embedding API call failed, retrying ({}/{})", retries, embeddingConfig.getMaxRetries());
                try {
                    TimeUnit.MILLISECONDS.sleep(embeddingConfig.getRetryDelayMs() * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
        throw new RuntimeException("Max retries exceeded for embedding API");
    }

    private List<List<Float>> callApi(List<String> texts) {
        // Real API call (DashScope)
        JSONObject input = new JSONObject();
        input.set("texts", texts);

        JSONObject body = new JSONObject();
        body.set("model", embeddingConfig.getModel());
        body.set("input", input);
        
        JSONObject parameters = new JSONObject();
        parameters.set("text_type", "document");
        body.set("parameters", parameters);

        try (HttpResponse response = HttpRequest.post(API_URL)
                .header("Authorization", "Bearer " + embeddingConfig.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(10000)
                .execute()) {

            if (!response.isOk()) {
                throw new RuntimeException("API Error: " + response.getStatus() + " " + response.body());
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.containsKey("output") && json.getJSONObject("output").containsKey("embeddings")) {
                JSONArray embeddings = json.getJSONObject("output").getJSONArray("embeddings");
                
                // Sort by text_index to ensure order
                List<JSONObject> embeddingObjects = new ArrayList<>();
                for (int i = 0; i < embeddings.size(); i++) {
                    embeddingObjects.add(embeddings.getJSONObject(i));
                }
                embeddingObjects.sort(Comparator.comparingInt(o -> o.getInt("text_index")));

                List<List<Float>> result = new ArrayList<>();
                for (JSONObject item : embeddingObjects) {
                    JSONArray vec = item.getJSONArray("embedding");
                    List<Float> floatVec = vec.toList(Float.class);
                    result.add(floatVec);
                }
                return result;
            } else {
                throw new RuntimeException("Invalid API response format: " + response.body());
            }
        }
    }
}
