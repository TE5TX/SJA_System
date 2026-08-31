package com.sja.system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=sja-test-secret-must-be-at-least-32-bytes-long",
        "sja.async-task.enabled=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class SjaSystemApplicationTests {

    @Test
    void contextLoads() {
    }

}
