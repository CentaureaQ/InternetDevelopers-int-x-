package com.sspku.agent.common.exception;

import com.sspku.agent.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ApiResponse<?> handleJdbcConnectionException(CannotGetJdbcConnectionException e) {
        log.error("数据库连接失败", e);
        return ApiResponse.fail(500, "数据库连接失败，请检查数据库服务与配置");
    }

    @ExceptionHandler({DataAccessException.class, PersistenceException.class, MyBatisSystemException.class})
    public ApiResponse<?> handleDataAccessRelated(Exception e) {
        String root = getRootMessage(e);
        log.error("数据库访问异常: {}", root, e);
        return ApiResponse.fail(500, "数据库访问异常: " + root);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验失败: {}", message);
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数绑定失败: {}", message);
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        // 开发环境返回详细错误信息，生产环境返回通用错误
        String message = "系统错误，请联系管理员";
        String activeProfile = System.getProperty("spring.profiles.active", "");
        if (activeProfile.contains("dev") || activeProfile.isEmpty()) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && !errorMsg.isEmpty()) {
                message = "系统错误: " + errorMsg;
            }
        }
        return ApiResponse.fail(999, message);
    }

    private String getRootMessage(Throwable t) {
        Throwable cur = t;
        String msg = cur.getMessage();
        while (cur.getCause() != null) {
            cur = cur.getCause();
            if (cur.getMessage() != null) {
                msg = cur.getMessage();
            }
        }
        return msg == null ? "未知错误" : msg;
    }
}

