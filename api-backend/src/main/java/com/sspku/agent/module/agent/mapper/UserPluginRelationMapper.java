package com.sspku.agent.module.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-插件关联 Mapper
 */
@Mapper
public interface UserPluginRelationMapper {

    void insertOrUpdate(@Param("userId") Long userId, @Param("pluginIds") List<Long> pluginIds);
}
