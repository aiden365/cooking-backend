package com.cooking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {RedisVectorStoreAutoConfiguration.class})
@EnableTransactionManagement()
@MapperScan("com.cooking.core.mapper")
public class CookingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CookingBackendApplication.class, args);
    }

}
