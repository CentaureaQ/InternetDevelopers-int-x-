package com.sspku.agent.module.knowledge.service.impl;

import com.sspku.agent.module.knowledge.dto.SearchResult;
import com.sspku.agent.module.knowledge.service.VectorStorageService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import io.milvus.param.RpcStatus;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorStorageServiceImpl implements VectorStorageService {

    private final MilvusServiceClient milvusClient;

    private String getCollectionName(Long kbId) {
        return "kb_" + kbId;
    }

    @Override
    public void createCollection(Long kbId, int dimension) {
        String collectionName = getCollectionName(kbId);
        
        try {
            R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (hasCollection.getData() != null && hasCollection.getData()) {
                return;
            }

            FieldType idField = FieldType.newBuilder()
                    .withName("chunk_id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName("vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType docIdField = FieldType.newBuilder()
                    .withName("doc_id")
                    .withDataType(DataType.Int64)
                    .build();

            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("Knowledge Base " + kbId)
                    .addFieldType(idField)
                    .addFieldType(vectorField)
                    .addFieldType(docIdField)
                    .build();

            R<RpcStatus> response = milvusClient.createCollection(createCollectionParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to create collection: " + response.getMessage());
            }

            // Create Index
            R<RpcStatus> indexResponse = milvusClient.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":1024}")
                    .withSyncMode(true)
                    .build());
            
            if (indexResponse.getStatus() != R.Status.Success.getCode()) {
                 log.warn("Failed to create index: {}", indexResponse.getMessage());
            }
            
            // Load collection
            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error creating collection for KB {}", kbId, e);
            throw new RuntimeException("Failed to initialize vector store", e);
        }
    }

    @Override
    public void insertVectors(Long kbId, List<Long> chunkIds, List<List<Float>> vectors, List<Long> docIds) {
        String collectionName = getCollectionName(kbId);
        
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("chunk_id", chunkIds));
        fields.add(new InsertParam.Field("vector", vectors));
        fields.add(new InsertParam.Field("doc_id", docIds));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<io.milvus.grpc.MutationResult> response = milvusClient.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to insert vectors: " + response.getMessage());
        }
    }

    @Override
    public List<SearchResult> search(Long kbId, List<Float> queryVector, int topK, double threshold) {
        String collectionName = getCollectionName(kbId);
        log.info("Searching Milvus collection: {}, topK: {}, threshold: {}", collectionName, topK, threshold);
        
        // Check if collection exists
        R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        if (hasCollection.getData() == null || !hasCollection.getData()) {
            log.warn("Collection {} does not exist", collectionName);
            return Collections.emptyList();
        }

        // Ensure collection is loaded
        try {
            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to load collection {}, it might be already loaded or busy", collectionName);
        }

        List<String> outFields = Collections.singletonList("doc_id");
        List<List<Float>> vectors = Collections.singletonList(queryVector);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withOutFields(outFields)
                .withTopK(topK)
                .withVectors(vectors)
                .withVectorFieldName("vector")
                .withParams("{\"nprobe\":10}")
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("Search failed: {}", response.getMessage());
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        
        log.info("Milvus returned {} raw results", scores.size());

        List<SearchResult> results = new ArrayList<>();
        for (SearchResultsWrapper.IDScore score : scores) {
            log.info("Result: id={}, score={}", score.getLongID(), score.getScore());
            if (score.getScore() < threshold) {
                continue;
            }
            SearchResult result = new SearchResult();
            result.setChunkId(score.getLongID());
            result.setScore(score.getScore());
            
            Object docIdObj = score.get("doc_id");
            if (docIdObj != null) {
                 result.setDocId(Long.parseLong(docIdObj.toString()));
            }
            results.add(result);
        }
        
        return results;
    }
}
