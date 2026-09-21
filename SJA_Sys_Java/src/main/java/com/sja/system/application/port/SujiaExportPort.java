package com.sja.system.application.port;

import com.sja.system.application.vo.tool.SujiaUserVO;

import java.util.List;

/**
 * Generates the workbook content required by the Sujia use case.
 */
public interface SujiaExportPort {

    byte[] write(List<SujiaUserVO> rows);
}
