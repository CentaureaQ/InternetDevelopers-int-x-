package com.sspku.agent.module.knowledge.service;

import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import java.util.List;

public interface KnowledgeBaseService {
    KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase);
    KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase);
    void deleteKnowledgeBase(Long id);
    KnowledgeBase getKnowledgeBaseById(Long id);
    KnowledgeBase getKnowledgeBaseByUuid(String uuid);
    List<KnowledgeBase> getAllKnowledgeBases();
    List<KnowledgeBase> getKnowledgeBasesByOwner(Long ownerId);
}
