package com.sspku.agent.module.knowledge.service;

import com.sspku.agent.module.knowledge.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeDocumentService {
    List<KnowledgeDocument> getDocuments(String kbUuid, String status);
    KnowledgeDocument uploadDocument(String kbUuid, MultipartFile file);
    KnowledgeDocument getDocument(String uuid);
    void deleteDocument(String uuid);
}
