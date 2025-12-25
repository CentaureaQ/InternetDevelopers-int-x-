package com.sspku.agent.module.plugin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.sspku.agent.common.exception.BusinessException;
import com.sspku.agent.module.plugin.dto.PluginCreateDTO;
import com.sspku.agent.module.plugin.dto.PluginUpdateDTO;
import com.sspku.agent.module.plugin.entity.Plugin;
import com.sspku.agent.module.plugin.mapper.PluginMapper;
import com.sspku.agent.module.plugin.service.IPluginService;
import com.sspku.agent.module.plugin.vo.PluginVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件服务实现类
 */
@Service
public class PluginServiceImpl implements IPluginService {

    @Resource
    private PluginMapper pluginMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PluginVO createPlugin(PluginCreateDTO createDTO) {
        // 检查名称是否重复
        if (pluginMapper.existsByName(createDTO.getName())) {
            throw new BusinessException("插件名称已存在");
        }

        Plugin plugin = new Plugin();
        BeanUtil.copyProperties(createDTO, plugin);
        
        // 确保 JSON 字段不为空字符串
        if (plugin.getConfig() == null || plugin.getConfig().trim().isEmpty()) {
            plugin.setConfig("{}");
        }
        if (plugin.getOpenapiSpec() == null || plugin.getOpenapiSpec().trim().isEmpty()) {
            plugin.setOpenapiSpec("{}");
        }
        
        // 设置默认值
        plugin.setType("custom"); // 默认为自定义插件
        plugin.setStatus("disabled"); // 默认为禁用状态
        
        pluginMapper.insert(plugin);
        
        return BeanUtil.copyProperties(plugin, PluginVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PluginVO updatePlugin(Long id, PluginUpdateDTO updateDTO) {
        Plugin plugin = pluginMapper.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }

        // 如果修改了名称，检查名称是否重复
        if (updateDTO.getName() != null && !updateDTO.getName().equals(plugin.getName())) {
            if (pluginMapper.existsByName(updateDTO.getName())) {
                throw new BusinessException("插件名称已存在");
            }
        }

        BeanUtil.copyProperties(updateDTO, plugin, "id", "createdAt", "updatedAt", "type");
        
        // 确保 JSON 字段不为空字符串
        if (plugin.getConfig() == null || plugin.getConfig().trim().isEmpty()) {
            plugin.setConfig("{}");
        }
        if (plugin.getOpenapiSpec() == null || plugin.getOpenapiSpec().trim().isEmpty()) {
            plugin.setOpenapiSpec("{}");
        }
        
        pluginMapper.updateById(plugin);
        
        return getPluginById(id); // 返回最新数据
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlugin(Long id) {
        Plugin plugin = pluginMapper.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }
        
        // 检查是否为内置插件
        if ("builtin".equals(plugin.getType())) {
            throw new BusinessException("内置插件不允许删除");
        }
        
        pluginMapper.deleteById(id);
    }

    @Override
    public PluginVO getPluginById(Long id) {
        Plugin plugin = pluginMapper.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }
        return BeanUtil.copyProperties(plugin, PluginVO.class);
    }

    @Override
    public List<PluginVO> listPlugins(String type, String status) {
        List<Plugin> plugins;
        
        if (type != null) {
            plugins = pluginMapper.selectByType(type);
        } else if (status != null) {
            plugins = pluginMapper.selectByStatus(status);
        } else {
            plugins = pluginMapper.selectAllActive();
        }
        
        return plugins.stream()
                .map(p -> BeanUtil.copyProperties(p, PluginVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enablePlugin(Long id) {
        Plugin plugin = pluginMapper.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }
        pluginMapper.updateStatus(id, "enabled");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePlugin(Long id) {
        Plugin plugin = pluginMapper.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }
        pluginMapper.updateStatus(id, "disabled");
    }
}

