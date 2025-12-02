package com.sspku.agent.module.plugin.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.module.plugin.core.PluginExecutor;
import com.sspku.agent.module.plugin.dto.PluginCreateDTO;
import com.sspku.agent.module.plugin.dto.PluginUpdateDTO;
import com.sspku.agent.module.plugin.service.IPluginService;
import com.sspku.agent.module.plugin.vo.PluginVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件管理控制器
 */
@RestController
@RequestMapping("/api/plugin")
public class PluginController {

    @Resource
    private IPluginService pluginService;

    @Resource
    private PluginExecutor pluginExecutor;

    /**
     * 创建插件
     */
    @PostMapping
    public ApiResponse<PluginVO> createPlugin(@Valid @RequestBody PluginCreateDTO createDTO) {
        PluginVO pluginVO = pluginService.createPlugin(createDTO);
        return ApiResponse.ok(pluginVO);
    }

    /**
     * 更新插件
     */
    @PutMapping("/{id}")
    public ApiResponse<PluginVO> updatePlugin(@PathVariable Long id, @RequestBody PluginUpdateDTO updateDTO) {
        PluginVO pluginVO = pluginService.updatePlugin(id, updateDTO);
        return ApiResponse.ok(pluginVO);
    }

    /**
     * 删除插件
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlugin(@PathVariable Long id) {
        pluginService.deletePlugin(id);
        return ApiResponse.ok(null);
    }

    /**
     * 获取插件详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PluginVO> getPlugin(@PathVariable Long id) {
        PluginVO pluginVO = pluginService.getPluginById(id);
        return ApiResponse.ok(pluginVO);
    }

    /**
     * 查询插件列表
     */
    @GetMapping
    public ApiResponse<List<PluginVO>> listPlugins(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        List<PluginVO> list = pluginService.listPlugins(type, status);
        return ApiResponse.ok(list);
    }

    /**
     * 启用插件
     */
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enablePlugin(@PathVariable Long id) {
        pluginService.enablePlugin(id);
        return ApiResponse.ok(null);
    }

    /**
     * 禁用插件
     */
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disablePlugin(@PathVariable Long id) {
        pluginService.disablePlugin(id);
        return ApiResponse.ok(null);
    }

    /**
     * 执行插件
     */
    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> executePlugin(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        String functionName = (String) request.get("functionName");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        String result = pluginExecutor.execute(id, functionName, arguments);
        
        return ApiResponse.ok(Map.of(
                "pluginId", id,
                "functionName", functionName,
                "result", result
        ));
    }
}

