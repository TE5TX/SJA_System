package com.sja.system.application.dto.auth;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresCaptchaTokenForLogin() {
        LoginDTO dto = login("2026000000001", "abc123", null);
        assertFalse(validator.validate(dto).isEmpty());

        dto.setCaptchaToken("captcha-id");
        assertTrue(validator.validate(dto).isEmpty());
    }

    private LoginDTO login(String username, String password, String captchaToken) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setCaptchaToken(captchaToken);
        return dto;
    }
}
