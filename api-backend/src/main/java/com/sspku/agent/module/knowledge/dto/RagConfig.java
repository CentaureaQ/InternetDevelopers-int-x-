package com.sspku.agent.module.knowledge.dto;

import lombok.Data;

@Data
public class RagConfig {
    private int topK = 3;
    private double threshold = 0.4; // Lowered from 0.6 to allow more matches
    private int maxContextLength = 2000;
    private String similarityMetric = "cosine"; // cosine, euclidean, dot
}
