package com.sja.system.mapper;

import com.sja.system.vo.MonthlyFeeComponentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyFeeExportMapper {

    @Select({
            "<script>",
            "SELECT fee.user_id AS userId, fee.fee_amount AS feeAmount",
            "FROM (",
            "  SELECT c.user_id, c.fee_amount, a.publish_date AS fee_date, 1 AS work_order, c.id AS contributor_id",
            "  FROM biz_work_contributor c JOIN biz_editorial_article a ON c.work_type='EDITORIAL' AND c.work_id=a.id",
            "  WHERE a.is_valid=1 AND a.audit_status IN ('SUBMITTED','APPROVED') AND a.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, p.publish_date, 2, c.id",
            "  FROM biz_work_contributor c JOIN biz_photography_work p ON c.work_type='PHOTO' AND c.work_id=p.id",
            "  WHERE p.is_valid=1 AND p.audit_status IN ('SUBMITTED','APPROVED') AND p.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, v.publish_date, 3, c.id",
            "  FROM biz_work_contributor c JOIN biz_visual_work v ON c.work_type='VISUAL' AND c.work_id=v.id",
            "  WHERE v.is_valid=1 AND v.audit_status IN ('SUBMITTED','APPROVED') AND v.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, w.publish_date, 4, c.id",
            "  FROM biz_work_contributor c JOIN biz_video_work w ON c.work_type='VIDEO' AND c.work_id=w.id",
            "  WHERE w.is_valid=1 AND w.audit_status IN ('SUBMITTED','APPROVED') AND w.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, STR_TO_DATE(CONCAT(s.subsidy_month, '-01'), '%Y-%m-%d'), 5, c.id",
            "  FROM biz_work_contributor c JOIN biz_monthly_subsidy s ON c.work_type='SUBSIDY' AND c.work_id=s.id",
            "  WHERE s.is_valid=1 AND s.audit_status IN ('SUBMITTED','APPROVED') AND s.subsidy_month=#{month}",
            ") fee",
            "WHERE fee.fee_amount IS NOT NULL AND fee.fee_amount &gt; 0",
            "ORDER BY fee.fee_date, fee.work_order, fee.contributor_id",
            "</script>"
    })
    List<MonthlyFeeComponentVO> selectMonthlyComponents(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("month") String month);
}
