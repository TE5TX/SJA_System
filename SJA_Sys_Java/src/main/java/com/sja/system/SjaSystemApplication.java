package com.sja.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// 202609211427 - 完成了Mapper扫描路径迁移至DAL层，统一数据库访问组件边界。
@MapperScan("com.sja.system.dal.mysql")
@EnableScheduling
public class SjaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SjaSystemApplication.class, args);
    }

}
