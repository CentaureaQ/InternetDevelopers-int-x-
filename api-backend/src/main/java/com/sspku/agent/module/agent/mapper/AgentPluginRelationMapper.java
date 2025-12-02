package com.sspku.agent.module.agent.mapper;

import com.sspku.agent.module.agent.entity.AgentPluginRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能体-插件关联 Mapper
 */
@Mapper
public interface AgentPluginRelationMapper {

    void deleteByAgentId(@Param("agentId") Long agentId);

    void insertBatch(@Param("agentId") Long agentId, @Param("pluginIds") List<Long> pluginIds);

    List<Long> selectPluginIdsByAgentId(@Param("agentId") Long agentId);

    List<AgentPluginRelation> selectByAgentIds(@Param("agentIds") List<Long> agentIds);
}
