package com.sspku.agent.module.knowledge.service.impl;

import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import com.sspku.agent.module.knowledge.mapper.KnowledgeBaseMapper;
import com.sspku.agent.module.knowledge.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    @Transactional
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.getUuid() == null) {
            knowledgeBase.setUuid(UUID.randomUUID().toString());
        }
        // Set default values if not provided
        if (knowledgeBase.getLevel() == null) knowledgeBase.setLevel("personal");
        if (knowledgeBase.getDocumentCount() == null) knowledgeBase.setDocumentCount(0);
        if (knowledgeBase.getChunkCount() == null) knowledgeBase.setChunkCount(0);
        if (knowledgeBase.getTotalSize() == null) knowledgeBase.setTotalSize(0L);
        
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    @Transactional
    public KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase) {
        knowledgeBaseMapper.update(knowledgeBase);
        return knowledgeBaseMapper.selectById(knowledgeBase.getId());
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseByUuid(String uuid) {
        return knowledgeBaseMapper.selectByUuid(uuid);
    }

    @Override
    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseMapper.selectAll();
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBasesByOwner(Long ownerId) {
        return knowledgeBaseMapper.selectByOwnerId(ownerId);
    }
}
