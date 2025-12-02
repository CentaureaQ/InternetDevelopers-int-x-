package com.sspku.agent.module.user.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.module.user.dto.UserLoginDTO;
import com.sspku.agent.module.user.dto.UserRegisterDTO;
import com.sspku.agent.module.user.service.IUserService;
import com.sspku.agent.module.user.vo.UserVO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    @Resource
    private final IUserService userService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Validated @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return ApiResponse.ok();
    }
    @PostMapping("/login")
    public ApiResponse<UserVO> login(@Validated @RequestBody UserLoginDTO dto) {
        UserVO userVO = userService.login(dto);
        return ApiResponse.ok("登录成功", userVO);
    }
    @GetMapping("/info")
    public ApiResponse<UserVO> getUserInfo() {
        UserVO userVO = userService.getCurrentUserInfo();
        return ApiResponse.ok(userVO);
    }
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUserById(@PathVariable Long id) {
        UserVO userVO = userService.getUserById(id);
        return ApiResponse.ok(userVO);
    }
}

