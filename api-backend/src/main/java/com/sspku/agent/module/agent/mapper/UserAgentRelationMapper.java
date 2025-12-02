package com.sspku.agent.module.agent.mapper;

import com.sspku.agent.module.agent.entity.UserAgentRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-智能体关联 Mapper
 */
@Mapper
public interface UserAgentRelationMapper {

    void upsertOwner(@Param("userId") Long userId, @Param("agentId") Long agentId);

    Long selectOwnerId(@Param("agentId") Long agentId);

    List<UserAgentRelation> selectByAgentIds(@Param("agentIds") List<Long> agentIds);
}
