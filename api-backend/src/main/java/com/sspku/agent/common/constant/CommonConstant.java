package com.sspku.agent.common.constant;

/**
 * 通用常量
 */
public class CommonConstant {
    
    /** 用户状态：正常 */
    public static final int USER_STATUS_NORMAL = 1;
    
    /** 用户状态：禁用 */
    public static final int USER_STATUS_DISABLED = 0;
    
    /** 删除标记：未删除 */
    public static final int NOT_DELETED = 0;
    
    /** 删除标记：已删除 */
    public static final int DELETED = 1;
    
    /** 聊天会话状态：进行中 */
    public static final int CHAT_SESSION_ACTIVE = 1;
    
    /** 聊天会话状态：已结束 */
    public static final int CHAT_SESSION_ENDED = 0;
    
    /** 聊天角色：用户 */
    public static final String CHAT_ROLE_USER = "user";
    
    /** 聊天角色：AI助手 */
    public static final String CHAT_ROLE_ASSISTANT = "assistant";
    
    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;
    
    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;
}

