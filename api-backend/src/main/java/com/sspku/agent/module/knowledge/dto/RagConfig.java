package com.sspku.agent.module.knowledge.dto;

import lombok.Data;

@Data
public class RagConfig {
    private int topK = 3;
    private double threshold = 0.6;
    private int maxContextLength = 2000;
    private String similarityMetric = "cosine"; // cosine, euclidean, dot
}
