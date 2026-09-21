package com.sja.system.application.dto.user;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUpdateDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsLettersAndDigitsWithoutRequiringMixedLetterCase() {
        assertTrue(validator.validate(password("abc12345")).isEmpty());
        assertTrue(validator.validate(password("ABC12345")).isEmpty());
    }

    @Test
    void rejectsPasswordsWithoutBothLettersAndDigits() {
        assertFalse(validator.validate(password("abcdefgh")).isEmpty());
        assertFalse(validator.validate(password("12345678")).isEmpty());
        assertFalse(validator.validate(password("abc123!@")).isEmpty());
    }

    private PasswordUpdateDTO password(String newPassword) {
        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setOldPassword("oldPassword1");
        dto.setNewPassword(newPassword);
        return dto;
    }
}
