package com.sspku.agent.module.user.mapper;

import com.sspku.agent.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);
    
    /**
     * 根据ID查询用户(MyBatis-Plus风格)
     */
    User selectById(@Param("id") Long id);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 查询所有用户
     */
    List<User> findAll();
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 更新用户(MyBatis-Plus风格)
     */
    int updateById(User user);
    
    /**
     * 删除用户（逻辑删除）
     */
    int deleteById(@Param("id") Long id);
}

