package com.sspku.agent.module.user.service;

import com.sspku.agent.module.user.dto.UserLoginDTO;
import com.sspku.agent.module.user.dto.UserRegisterDTO;
import com.sspku.agent.module.user.vo.UserVO;

/**
 * 用户服务接口
 */
public interface IUserService {
    
    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);
    
    /**
     * 用户登录
     */
    UserVO login(UserLoginDTO dto);
    
    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUserInfo();
    
    /**
     * 根据ID获取用户信息
     */
    UserVO getUserById(Long userId);
}

