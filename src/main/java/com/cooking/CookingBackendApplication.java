package com.cooking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cooking.core.mapper")
public class CookingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CookingBackendApplication.class, args);
    }

}
