package com.sja.system.common;

import com.sja.system.vo.UserVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MaskingUtilsTest {

    @Test
    void masksUserListFieldsAndLogValues() {
        UserVO user = new UserVO();
        user.setPhone("13812345678");
        user.setIdCard("440101200001011234");
        user.setBankCard("6222021234567890123");
        user.setDormAddress("香晖苑1栋101");

        MaskingUtils.maskUser(user);

        assertEquals("138****5678", user.getPhone());
        assertEquals("4401****1234", user.getIdCard());
        assertEquals("6222****0123", user.getBankCard());
        assertEquals("香晖苑***", user.getDormAddress());

        String sanitized = MaskingUtils.sanitizeLogDetail(
                "idCard [440101200001011234]->[440101200001015678]; phone [13812345678]->[13912345678]");
        assertFalse(sanitized.contains("440101200001011234"));
        assertFalse(sanitized.contains("13812345678"));

        String json = MaskingUtils.sanitizeLogDetail(
                "{\"phone\":\"13812345678\",\"bankCard\":\"6222021234567890123\"}");
        assertFalse(json.contains("13812345678"));
        assertFalse(json.contains("6222021234567890123"));
    }
}
