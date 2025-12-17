package com.sspku.agent.module.knowledge.dto;

import lombok.Data;

@Data
public class RagConfig {
    private int topK = 3;
    private double threshold = 0.2; // Lowered to allow more matches for better recall
    private int maxContextLength = 2000;
    private String similarityMetric = "cosine"; // cosine, euclidean, dot
}
