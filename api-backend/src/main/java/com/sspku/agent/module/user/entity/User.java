package com.sspku.agent.module.user.entity;

import lombok.Data;

/**
 * 用户实体类
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
}
