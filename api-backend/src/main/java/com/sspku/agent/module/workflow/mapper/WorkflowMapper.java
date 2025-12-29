package com.sspku.agent.module.workflow.mapper;

import com.sspku.agent.module.workflow.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowMapper {

    int insert(Workflow workflow);

    int update(Workflow workflow);

    Workflow selectByIdAndOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    Workflow selectById(@Param("id") Long id);

    int deleteByIdAndOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    int updateStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId, @Param("status") String status);

    long countByCondition(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword,
            @Param("status") String status);

    List<Workflow> selectPageByCondition(
            @Param("ownerUserId") Long ownerUserId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
