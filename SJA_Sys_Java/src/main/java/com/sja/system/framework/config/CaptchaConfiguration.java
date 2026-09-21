package com.sja.system.framework.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaptchaConfiguration {

    private static final int CAPTCHA_BACKGROUND_COUNT = 20;

    @Bean
    public ResourceStore captchaResourceStore() {
        LocalMemoryResourceStore resourceStore = new LocalMemoryResourceStore();
        // 202608121738 - 使用20张校园及自然场景图片随机生成滑块验证码。
        for (int index = 1; index <= CAPTCHA_BACKGROUND_COUNT; index++) {
            resourceStore.addResource(
                    CaptchaTypeConstant.SLIDER,
                    new Resource("classpath", "captcha/background/" + index + ".jpg")
            );
        }
        return resourceStore;
    }
}
