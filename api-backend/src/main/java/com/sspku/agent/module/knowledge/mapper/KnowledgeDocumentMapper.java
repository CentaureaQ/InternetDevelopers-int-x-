package com.sspku.agent.module.knowledge.mapper;

import com.sspku.agent.module.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    int insert(KnowledgeDocument document);
    int update(KnowledgeDocument document);
    int deleteByUuid(String uuid);
    KnowledgeDocument selectByUuid(String uuid);
    List<KnowledgeDocument> selectByKnowledgeBaseId(@Param("kbId") Long kbId, @Param("status") String status);
    List<KnowledgeDocument> selectByKnowledgeBaseUuid(@Param("kbUuid") String kbUuid, @Param("status") String status);
}
