package com.sspku.agent.module.knowledge.mapper;

import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {
    int insert(KnowledgeChunk chunk);
    int batchInsert(@Param("list") List<KnowledgeChunk> list);
    List<KnowledgeChunk> selectByDocumentId(Long documentId);
    int deleteByDocumentId(Long documentId);
    List<KnowledgeChunk> selectByIds(@Param("ids") List<Long> ids);
}
