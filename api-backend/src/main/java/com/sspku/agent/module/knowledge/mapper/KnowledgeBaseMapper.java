package com.sspku.agent.module.knowledge.mapper;

import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {
    int insert(KnowledgeBase knowledgeBase);
    int update(KnowledgeBase knowledgeBase);
    int deleteById(Long id);
    KnowledgeBase selectByUuid(String uuid);
    KnowledgeBase selectById(Long id);
    List<KnowledgeBase> selectAll();
    List<KnowledgeBase> selectByOwnerId(Long ownerId);
}
