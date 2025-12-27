package com.sspku.agent.module.knowledge.mapper;

import com.sspku.agent.module.knowledge.entity.AgentKnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 智能体知识库关联Mapper
 */
@Mapper
public interface AgentKnowledgeRelationMapper {
    
    /**
     * 插入关联关系
     */
    int insert(AgentKnowledgeRelation relation);
    
    /**
     * 批量插入关联关系
     */
    int insertBatch(@Param("agentId") Long agentId, @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds);
    
    /**
     * 删除智能体的所有知识库关联
     */
    int deleteByAgentId(Long agentId);
    
    /**
     * 删除特定关联关系
     */
    int deleteByAgentIdAndKbId(@Param("agentId") Long agentId, @Param("kbId") Long kbId);
    
    /**
     * 查询智能体关联的所有知识库ID
     */
    List<Long> selectKbIdsByAgentId(Long agentId);
    
    /**
     * 查询知识库关联的所有智能体ID
     */
    List<Long> selectAgentIdsByKbId(Long kbId);
    
    /**
     * 查询智能体的所有知识库关联关系
     */
    List<AgentKnowledgeRelation> selectByAgentId(Long agentId);
}