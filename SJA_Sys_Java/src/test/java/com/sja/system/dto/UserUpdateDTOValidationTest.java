package com.sja.system.dto;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserUpdateDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteUserWithoutPhoto() {
        UserUpdateDTO dto = validDto();
        dto.setPhoto(null);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsMissingRequiredFieldAndInvalidStatus() {
        UserUpdateDTO dto = validDto();
        dto.setBankCard(" ");
        dto.setIsValid(2);

        assertFalse(validator.validate(dto).isEmpty());
    }

    private UserUpdateDTO validDto() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRealName("Test User");
        dto.setGender(1);
        dto.setStudentNo("20240001");
        dto.setIdCard("110101200001010011");
        dto.setPhone("13800138000");
        dto.setCollege("Test College");
        dto.setClassName("Test Class");
        dto.setDormAddress("Test Dormitory");
        dto.setBankCard("6222020000000000");
        dto.setDeptIds(List.of(1L));
        dto.setRoleIds(List.of(1L));
        dto.setIsValid(1);
        dto.setRemark("Normal");
        return dto;
    }
}
