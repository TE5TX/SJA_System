package com.sja.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.sja.system.mapper")
@EnableScheduling
public class SjaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SjaSystemApplication.class, args);
    }

}
