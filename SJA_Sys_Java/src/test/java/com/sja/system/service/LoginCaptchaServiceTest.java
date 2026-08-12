package com.sja.system.service;

import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

class LoginCaptchaServiceTest {

    @Test
    void returnsOneTimeTokenAfterSuccessfulVerification() {
        SecondaryVerificationApplication application = mock(SecondaryVerificationApplication.class);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        when(application.matching("captcha-id", track)).thenReturn(ApiResponse.ofSuccess());

        ApiResponse<?> response = new LoginCaptchaService(application).verify("captcha-id", track);

        assertTrue(response.isSuccess());
        assertEquals("captcha-id", ((java.util.Map<?, ?>) response.getData()).get("token"));
    }

    @Test
    void keepsCaptchaFailureCodeAndDoesNotReturnToken() {
        SecondaryVerificationApplication application = mock(SecondaryVerificationApplication.class);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        ApiResponse<?> failed = ApiResponse.ofCheckError("验证失败");
        doReturn(failed).when(application).matching("captcha-id", track);

        ApiResponse<?> response = new LoginCaptchaService(application).verify("captcha-id", track);

        assertEquals(failed.getCode(), response.getCode());
        assertFalse(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void consumesSecondaryVerificationOnlyOnce() {
        SecondaryVerificationApplication application = mock(SecondaryVerificationApplication.class);
        when(application.secondaryVerification("captcha-id")).thenReturn(true, false);
        LoginCaptchaService service = new LoginCaptchaService(application);

        assertTrue(service.consume("captcha-id"));
        assertFalse(service.consume("captcha-id"));
    }
}
