package com.sspku.agent.module.knowledge.service;

import com.sspku.agent.module.knowledge.dto.SearchResult;
import java.util.List;

public interface VectorStorageService {
    void createCollection(Long kbId, int dimension);
    void insertVectors(Long kbId, List<Long> chunkIds, List<List<Float>> vectors, List<Long> docIds);
    List<SearchResult> search(Long kbId, List<Float> queryVector, int topK, double threshold);
}
