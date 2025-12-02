package com.sspku.agent.module.user.service.impl;

import com.sspku.agent.common.exception.BusinessException;
import com.sspku.agent.common.util.JwtUtil;
import com.sspku.agent.module.user.dto.UserLoginDTO;
import com.sspku.agent.module.user.dto.UserRegisterDTO;
import com.sspku.agent.module.user.entity.User;
import com.sspku.agent.module.user.mapper.UserMapper;
import com.sspku.agent.module.user.service.IUserService;
import com.sspku.agent.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        // 检查用户是否存在
        User existUser = userMapper.findByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        userMapper.insert(user);
    }

    @Override
    public UserVO login(UserLoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构建返回对象
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .token(token)
                .build();
    }

    @Override
    public UserVO getCurrentUserInfo() {
        // 从安全上下文获取当前用户ID
        Long userId = jwtUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        return getUserById(userId);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }
}
