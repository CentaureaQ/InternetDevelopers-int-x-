package com.sspku.agent.module.agent.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型配置请求
 */
@Data
public class ModelConfigRequest {

    @NotBlank(message = "模型服务商不能为空")
    private String provider;

    @NotBlank(message = "模型名称不能为空")
    private String model;

    @DecimalMin(value = "0.0", message = "temperature不能小于0")
    @DecimalMax(value = "1.0", message = "temperature不能大于1")
    private Double temperature = 0.7;

    @Min(value = 32, message = "maxTokens不能小于32")
    @Max(value = 4096, message = "maxTokens不能超过4096")
    private Integer maxTokens = 2048;

    @DecimalMin(value = "0.0", message = "topP不能小于0")
    @DecimalMax(value = "1.0", message = "topP不能大于1")
    private Double topP = 0.9;
}
