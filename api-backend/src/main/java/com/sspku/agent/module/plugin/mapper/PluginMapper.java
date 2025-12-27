package com.sspku.agent.module.plugin.mapper;

import com.sspku.agent.module.plugin.entity.Plugin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 插件 Mapper 接口
 * 负责插件数据的持久化操作
 */
@Mapper
public interface PluginMapper {
    
    /**
     * 根据ID查询插件
     * @param id 插件ID
     * @return 插件对象，未找到返回null
     */
    Plugin selectById(@Param("id") Long id);
    
    /**
     * 根据名称查询插件
     * @param name 插件名称
     * @return 插件对象，未找到返回null
     */
    Plugin selectByName(@Param("name") String name);
    
    /**
     * 查询所有插件（包括已删除的）
     * @return 插件列表
     */
    List<Plugin> selectAll();
    
    /**
     * 查询未删除的插件列表
     * @return 插件列表
     */
    List<Plugin> selectAllActive();
    
    /**
     * 根据类型查询插件
     * @param type 插件类型（builtin/custom）
     * @return 插件列表
     */
    List<Plugin> selectByType(@Param("type") String type);
    
    /**
     * 根据状态查询插件
     * @param status 插件状态（enabled/disabled）
     * @return 插件列表
     */
    List<Plugin> selectByStatus(@Param("status") String status);
    
    /**
     * 查询启用的插件（未删除且状态为enabled）
     * @return 插件列表
     */
    List<Plugin> selectEnabledPlugins();
    
    /**
     * 插入新插件
     * @param plugin 插件对象
     * @return 影响的行数
     */
    int insert(Plugin plugin);
    
    /**
     * 更新插件信息
     * @param plugin 插件对象
     * @return 影响的行数
     */
    int updateById(Plugin plugin);
    
    /**
     * 更新插件状态
     * @param id 插件ID
     * @param status 新状态
     * @return 影响的行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 删除插件（物理删除，触发关联级联）
     */
    int deleteById(@Param("id") Long id);
    
    /**
    * 统计插件数量
    * @return 插件总数
     */
    int countActive();
    
    /**
     * 检查插件名称是否存在
     * @param name 插件名称
     * @return 存在返回true，否则返回false
     */
    boolean existsByName(@Param("name") String name);
}

