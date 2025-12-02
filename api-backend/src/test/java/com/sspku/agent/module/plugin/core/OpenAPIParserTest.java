package com.sspku.agent.module.plugin.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.module.plugin.dto.FunctionDefinition;
import com.sspku.agent.module.plugin.entity.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPIParserTest {

    private final OpenAPIParser parser = new OpenAPIParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parse() {
        // 准备测试数据：一个包含 path 参数和 requestBody 的 OpenAPI Spec
        String jsonSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "servers": [
                {
                  "url": "https://api.example.com"
                }
              ],
              "paths": {
                "/weather": {
                  "get": {
                    "operationId": "getWeather",
                    "summary": "获取天气",
                    "parameters": [
                      {
                        "name": "city",
                        "in": "query",
                        "required": true,
                        "schema": {
                          "type": "string"
                        },
                        "description": "城市名称"
                      }
                    ]
                  }
                },
                "/users/{id}": {
                  "post": {
                    "operationId": "updateUser",
                    "summary": "更新用户",
                    "parameters": [
                      {
                        "name": "id",
                        "in": "path",
                        "required": true,
                        "schema": {
                          "type": "integer"
                        },
                        "description": "用户ID"
                      }
                    ],
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "name": {
                                "type": "string",
                                "description": "新名称"
                              },
                              "email": {
                                "type": "string"
                              }
                            },
                            "required": ["name"]
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        Plugin plugin = new Plugin();
        plugin.setId(1L);
        plugin.setName("Test Plugin");
        plugin.setOpenapiSpec(jsonSpec);

        // 执行解析
        List<FunctionDefinition> functions = parser.parse(plugin);

        // 验证结果
        assertEquals(2, functions.size());

        // 验证 getWeather
        FunctionDefinition getWeather = functions.stream()
                .filter(f -> "getWeather".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        
        assertEquals("获取天气", getWeather.getDescription());
        assertTrue(getWeather.getParameters().getProperties().containsKey("city"));
        assertTrue(getWeather.getParameters().getRequired().contains("city"));
        assertEquals("GET", getWeather.getMetadata().getMethod());
        assertEquals("/weather", getWeather.getMetadata().getPath());
        assertEquals("https://api.example.com", getWeather.getMetadata().getBaseUrl());

        // 验证 updateUser
        FunctionDefinition updateUser = functions.stream()
                .filter(f -> "updateUser".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        
        // 验证参数合并（path参数 + body参数）
        assertTrue(updateUser.getParameters().getProperties().containsKey("id")); // path参数
        assertTrue(updateUser.getParameters().getProperties().containsKey("name")); // body参数
        assertTrue(updateUser.getParameters().getProperties().containsKey("email")); // body参数
        
        assertTrue(updateUser.getParameters().getRequired().contains("id"));
        assertTrue(updateUser.getParameters().getRequired().contains("name"));
        assertFalse(updateUser.getParameters().getRequired().contains("email"));
        
        assertEquals("POST", updateUser.getMetadata().getMethod());
    }
}

