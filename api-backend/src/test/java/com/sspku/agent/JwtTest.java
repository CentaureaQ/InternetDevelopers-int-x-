package com.sspku.agent;

import com.sspku.agent.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具类测试
 */
@SpringBootTest
public class JwtTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void testGenerateAndValidateToken() {
        Long userId = 1L;
        String username = "testuser";

        // 生成Token
        String token = jwtUtil.generateToken(userId, username);
        assertNotNull(token, "Token应该不为空");
        assertFalse(token.isEmpty(), "Token应该不为空字符串");

        // 验证Token
        assertTrue(jwtUtil.validateToken(token), "Token应该有效");

        // 从Token中提取信息
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        assertEquals(userId, extractedUserId, "用户ID应该匹配");
        assertEquals(username, extractedUsername, "用户名应该匹配");
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.validateToken(invalidToken), "无效Token应该返回false");
    }

    @Test
    public void testEmptyToken() {
        assertFalse(jwtUtil.validateToken(""), "空Token应该返回false");
        assertFalse(jwtUtil.validateToken(null), "null Token应该返回false");
    }
}



