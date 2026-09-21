package com.sja.system.application.dto.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCreateDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsStudentNumberAsTheOnlyRequiredField() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setStudentNo("2026000000003");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsMissingStudentNumber() {
        assertFalse(validator.validate(new UserCreateDTO()).isEmpty());
    }
}
