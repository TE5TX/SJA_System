package com.sja.system.config;

import com.sja.system.infrastructure.web.CurrentActorArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 参数适配配置。
 */
@Configuration
@RequiredArgsConstructor
// 202609211059 - 完成了当前操作人参数解析器的统一注册。
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final CurrentActorArgumentResolver currentActorArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentActorArgumentResolver);
    }
}
