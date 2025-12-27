package com.sspku.agent.config;

import com.sspku.agent.common.util.JwtUtil;
import com.sspku.agent.module.user.entity.User;
import com.sspku.agent.module.user.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 简单的 JWT 认证过滤器：读取 Authorization: Bearer <token>，解析 userId 并从数据库加载 User，
 * 将其设置为 SecurityContext 的 Authentication，以便控制器中的 @AuthenticationPrincipal 注入生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                String lower = auth.toLowerCase();
                if (lower.startsWith("bearer ")) {
                    String token = auth.substring(7).trim();
                    if (jwtUtil.validateToken(token)) {
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        if (userId != null) {
                            User user = userMapper.selectById(userId);
                            if (user != null) {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("JwtAuthenticationFilter 解析 token 失败：{}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
