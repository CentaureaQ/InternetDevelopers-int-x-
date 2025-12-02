package com.sspku.agent.module.agent.mapper;

import com.sspku.agent.module.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能体Mapper
 */
@Mapper
public interface AgentMapper {

    int insert(Agent agent);

    int update(Agent agent);

    Agent selectById(@Param("id") Long id);

    long countByCondition(@Param("keyword") String keyword, @Param("status") String status);

    List<Agent> selectPageByCondition(@Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    int updateStatus(Agent agent);
}

