package com.sja.system.framework.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.CrudResourceStore;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaptchaConfigurationTest {

    @Test
    void registersTwentyCaptchaBackgroundsWithExpectedDimensions() throws Exception {
        ResourceStore resourceStore = new CaptchaConfiguration().captchaResourceStore();
        List<Resource> resources = ((CrudResourceStore) resourceStore)
                .listResourcesByTypeAndTag(CaptchaTypeConstant.SLIDER, "default");

        assertEquals(20, resources.size());
        for (Resource resource : resources) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource.getData())) {
                assertNotNull(input, resource.getData());
                BufferedImage image = ImageIO.read(input);
                assertNotNull(image, resource.getData());
                assertEquals(600, image.getWidth(), resource.getData());
                assertEquals(360, image.getHeight(), resource.getData());
            }
        }
    }
}
