package com.sspku.agent.module.knowledge.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.sspku.agent.module.knowledge.component.chunker.Chunker;
import com.sspku.agent.module.knowledge.component.chunker.ChunkerFactory;
import com.sspku.agent.module.knowledge.component.chunker.ChunkingConfig;
import com.sspku.agent.module.knowledge.config.EmbeddingConfig;
import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;
import com.sspku.agent.module.knowledge.entity.KnowledgeDocument;
import com.sspku.agent.module.knowledge.mapper.KnowledgeBaseMapper;
import com.sspku.agent.module.knowledge.mapper.KnowledgeChunkMapper;
import com.sspku.agent.module.knowledge.mapper.KnowledgeDocumentMapper;
import com.sspku.agent.module.knowledge.service.EmbeddingService;
import com.sspku.agent.module.knowledge.service.KnowledgeDocumentService;
import com.sspku.agent.module.knowledge.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final ChunkerFactory chunkerFactory;
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;
    private final EmbeddingConfig embeddingConfig;

    @Value("${app.upload.path:/app/uploads}")
    private String uploadPath;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("txt", "md", "markdown");

    @Override
    public List<KnowledgeDocument> getDocuments(String kbUuid, String status) {
        return documentMapper.selectByKnowledgeBaseUuid(kbUuid, status);
    }

    @Override
    @Transactional
    public KnowledgeDocument uploadDocument(String kbUuid, MultipartFile file) {
        // 1. Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds limit (10MB)");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only TXT and Markdown files are supported");
        }

        // 2. Check Knowledge Base
        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null) {
            throw new IllegalArgumentException("Knowledge Base not found");
        }

        // 3. Save file
        String uuid = IdUtil.simpleUUID();
        String fileName = uuid + "." + ext;
        File dest = new File(uploadPath, fileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new RuntimeException("Failed to save file");
        }

        // 4. Save DB record
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUuid(uuid);
        doc.setName(originalFilename);
        doc.setFilePath(dest.getAbsolutePath());
        doc.setFileSize(file.getSize());
        doc.setFileType(ext);
        doc.setKnowledgeBaseId(kb.getId());
        doc.setStatus("uploading");
        
        documentMapper.insert(doc);

        // Update KB stats (doc count +1, size +fileSize)
        knowledgeBaseMapper.updateStats(kb.getId(), 1, 0, file.getSize());

        // 5. Async process
        // Note: Calling async method from within the same class won't work with default proxy.
        // Ideally, this should be in a separate service or self-injected.
        // For simplicity in this step, I'll leave it but it might run synchronously if called directly.
        // To fix, I should move it to a separate component or use AopContext.currentProxy() if enabled.
        // Or better, just let the controller call it? No, upload should trigger it.
        // I'll use a separate method in a different bean or just accept it runs sync for now unless I fix it.
        // Actually, let's just run it in a new thread for now to simulate async without complex setup, 
        // or assume @Async is working if I self-inject.
        // I'll use a simple thread for now to ensure it's async without dependency issues.
        new Thread(() -> processDocumentAsync(doc)).start();

        return doc;
    }

    public void processDocumentAsync(KnowledgeDocument doc) {
        try {
            // Update status to processing
            doc.setStatus("processing");
            documentMapper.update(doc);

            // Read file content
            File file = new File(doc.getFilePath());
            String content = FileUtil.readUtf8String(file);

            // Determine config
            ChunkingConfig config = new ChunkingConfig();
            // Simple logic for chunk size based on file size
            if (doc.getFileSize() < 5 * 1024) {
                config.setChunkSize(500);
                config.setChunkOverlap(50);
            } else if (doc.getFileSize() > 50 * 1024) {
                config.setChunkSize(1000);
                config.setChunkOverlap(100);
            }
            
            String strategy = "sliding_window";
            if ("md".equalsIgnoreCase(doc.getFileType()) || "markdown".equalsIgnoreCase(doc.getFileType())) {
                strategy = "markdown_aware";
            }
            config.setChunkStrategy(strategy);

            // Chunk
            Chunker chunker = chunkerFactory.getChunker(strategy);
            List<String> chunks = chunker.chunk(content, config);

            // Update doc info
            doc.setChunkCount(chunks.size());
            log.info("Document {} chunked into {} parts using strategy {}", doc.getName(), chunks.size(), strategy);

            // Save chunks to DB
            List<KnowledgeChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setKbId(doc.getKnowledgeBaseId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setTokenCount(chunks.get(i).length()); // Approximation
                chunkEntities.add(chunk);
            }
            
            if (!chunkEntities.isEmpty()) {
                chunkMapper.batchInsert(chunkEntities);
                // Update KB stats (chunk count)
                knowledgeBaseMapper.updateStats(doc.getKnowledgeBaseId(), 0, chunks.size(), 0);
            }

            // Vectorize
            List<List<Float>> embeddings = embeddingService.embedDocuments(chunks);
            
            // Store in Milvus
            if (!embeddings.isEmpty()) {
                // Ensure collection exists
                vectorStorageService.createCollection(doc.getKnowledgeBaseId(), embeddingConfig.getDimension());
                
                List<Long> chunkIds = new ArrayList<>();
                List<Long> docIds = new ArrayList<>();
                for (KnowledgeChunk chunk : chunkEntities) {
                    chunkIds.add(chunk.getId());
                    docIds.add(doc.getId());
                }
                
                vectorStorageService.insertVectors(doc.getKnowledgeBaseId(), chunkIds, embeddings, docIds);
            }

            // Update status to processed
            doc.setStatus("processed");
            doc.setProcessedAt(LocalDateTime.now());
            documentMapper.update(doc);
            
        } catch (Exception e) {
            log.error("Failed to process document: {}", doc.getUuid(), e);
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.update(doc);
        }
    }

    @Override
    public KnowledgeDocument getDocument(String uuid) {
        return documentMapper.selectByUuid(uuid);
    }

    @Override
    @Transactional
    public void deleteDocument(String uuid) {
        KnowledgeDocument doc = documentMapper.selectByUuid(uuid);
        if (doc != null) {
            // Delete file
            FileUtil.del(doc.getFilePath());
            // Delete DB record
            documentMapper.deleteByUuid(uuid);
            
            // Update KB stats
            knowledgeBaseMapper.updateStats(doc.getKnowledgeBaseId(), -1, -doc.getChunkCount(), -doc.getFileSize());
            
            // TODO: Delete vector data
        }
    }
}
