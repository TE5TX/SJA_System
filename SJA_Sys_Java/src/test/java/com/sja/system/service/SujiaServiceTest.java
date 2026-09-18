package com.sja.system.service;

import com.sja.system.application.model.SujiaExportFile;
import com.sja.system.application.port.SujiaExportPort;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.vo.SujiaUserVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SujiaServiceTest {

    @Test
    void resolvesAllNamesWithOneUserQueryAndPreservesInputOrder() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        SujiaExportPort exportPort = mock(SujiaExportPort.class);
        SujiaService service = new SujiaService(userMapper, userDeptMapper, exportPort);
        SysUser zhang = user(1L, "张三", "20260002");
        SysUser li = user(2L, "李四", "20260001");
        when(userMapper.selectList(any())).thenReturn(List.of(li, zhang));

        List<SujiaUserVO> result = service.resolveNames(
                List.of(" 张三 ", "李四", "不存在", "张三"), 7L, 0);

        assertEquals(List.of("张三", "李四", "不存在"),
                result.stream().map(SujiaUserVO::getRealName).toList());
        assertTrue(result.get(0).getMatched());
        assertFalse(result.get(2).getMatched());
        verify(userMapper, times(1)).selectList(any());
    }

    @Test
    void delegatesWorkbookGenerationToTheExportPort() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SujiaExportPort exportPort = mock(SujiaExportPort.class);
        SujiaService service = new SujiaService(
                userMapper, mock(SysUserDeptMapper.class), exportPort);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L, "张三", "20260002")));
        when(exportPort.write(any())).thenReturn(new byte[]{1, 2, 3});

        SujiaExportFile file = service.export(List.of("张三"), 7L, 0);

        assertEquals("素拓名单.xlsx", file.fileName());
        assertArrayEquals(new byte[]{1, 2, 3}, file.content());
        verify(exportPort).write(any());
    }

    @Test
    void doesNotQueryUsersWhenARestrictedOperatorHasNoDepartment() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        when(userDeptMapper.selectList(any())).thenReturn(List.of());
        SujiaService service = new SujiaService(userMapper, userDeptMapper, mock(SujiaExportPort.class));

        List<SujiaUserVO> result = service.resolveNames(List.of("张三"), 7L, 5);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getMatched());
        verifyNoInteractions(userMapper);
    }

    private SysUser user(Long id, String realName, String studentNo) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        return user;
    }
}
