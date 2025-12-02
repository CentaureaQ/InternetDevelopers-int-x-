package com.sspku.agent.module.plugin.service;

import com.sspku.agent.module.plugin.dto.PluginCreateDTO;
import com.sspku.agent.module.plugin.dto.PluginUpdateDTO;
import com.sspku.agent.module.plugin.vo.PluginVO;

import java.util.List;

/**
 * 插件服务接口
 */
public interface IPluginService {
    
    /**
     * 创建插件
     */
    PluginVO createPlugin(PluginCreateDTO createDTO);
    
    /**
     * 更新插件
     */
    PluginVO updatePlugin(Long id, PluginUpdateDTO updateDTO);
    
    /**
     * 删除插件（逻辑删除）
     */
    void deletePlugin(Long id);
    
    /**
     * 根据ID获取插件
     */
    PluginVO getPluginById(Long id);
    
    /**
     * 查询插件列表
     * @param type 插件类型（可选）
     * @param status 插件状态（可选）
     */
    List<PluginVO> listPlugins(String type, String status);
    
    /**
     * 启用插件
     */
    void enablePlugin(Long id);
    
    /**
     * 禁用插件
     */
    void disablePlugin(Long id);
}

