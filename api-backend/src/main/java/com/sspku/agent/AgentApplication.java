package com.sspku.agent;

import io.jsonwebtoken.SignatureAlgorithm;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sspku.agent.module.*.mapper")
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
        System.out.println("===========================================");
        System.out.println("简单后端服务启动成功！");
        System.out.println("===========================================");
    }
}

