package com.sja.system.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "knife4j.enable", havingValue = "true")
public class Knife4jConfig {

    @Bean
    public OpenAPI sjaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SJA System API")
                .description("校记者团综合管理系统接口文档")
                .contact(new Contact().name("SJA Team"))
                .version("1.0"));
    }

}
