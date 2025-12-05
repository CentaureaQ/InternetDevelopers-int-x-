package com.sspku.agent.module.knowledge.mapper;

import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper {
    int insert(KnowledgeBase knowledgeBase);
    int update(KnowledgeBase knowledgeBase);
    int deleteById(Long id);
    KnowledgeBase selectByUuid(String uuid);
    KnowledgeBase selectById(Long id);
    List<KnowledgeBase> selectAll();
    List<KnowledgeBase> selectByOwnerId(Long ownerId);
    
    int updateStats(@Param("id") Long id, 
                   @Param("docCountDelta") int docCountDelta, 
                   @Param("chunkCountDelta") int chunkCountDelta, 
                   @Param("sizeDelta") long sizeDelta);
}
