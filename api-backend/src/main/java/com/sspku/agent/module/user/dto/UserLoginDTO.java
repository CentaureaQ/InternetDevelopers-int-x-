package com.sspku.agent.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}

