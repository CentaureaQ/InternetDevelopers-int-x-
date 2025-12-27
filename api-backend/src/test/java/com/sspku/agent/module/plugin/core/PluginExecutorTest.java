package com.sspku.agent.module.plugin.core;

import com.sspku.agent.module.plugin.dto.FunctionDefinition;
import com.sspku.agent.module.plugin.service.IPluginService;
import com.sspku.agent.module.plugin.vo.PluginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PluginExecutorTest {

    @InjectMocks
    private PluginExecutorImpl pluginExecutor;

    @Mock
    private IPluginService pluginService;

    @Mock
    private OpenAPIParser openAPIParser;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute() {
        // 1. 准备数据
        Long pluginId = 1L;
        String functionName = "getWeather";
        Map<String, Object> args = new HashMap<>();
        args.put("city", "Beijing");

        PluginVO pluginVO = new PluginVO();
        pluginVO.setId(pluginId);
        pluginVO.setName("Test Plugin");
        pluginVO.setStatus("enabled");
        pluginVO.setConfig("{\"apiKey\": \"123\"}");

        FunctionDefinition function = new FunctionDefinition();
        function.setName(functionName);
        FunctionDefinition.Metadata metadata = new FunctionDefinition.Metadata();
        metadata.setBaseUrl("https://api.weather.com");
        metadata.setPath("/current");
        metadata.setMethod("GET");
        function.setMetadata(metadata);

        // 2. Mock 行为
        when(pluginService.getPluginById(pluginId)).thenReturn(pluginVO);
        when(openAPIParser.parse(any())).thenReturn(List.of(function));
        
        String expectedResponse = "{\"temp\": 25}";
        when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // 3. 执行
        String result = pluginExecutor.execute(pluginId, functionName, args);

        // 4. 验证
        assertEquals(expectedResponse, result);
    }
}

