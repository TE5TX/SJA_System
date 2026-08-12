package com.sja.system.mapper;

import com.sja.system.vo.MonthlyFeeComponentVO;
import com.sja.system.vo.MonthlyEditorialPlatformVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyFeeExportMapper {

    // 202608121123 - 修复了月份稿费导出包含未审核通过任务的问题。
    @Select({
            "<script>",
            "SELECT fee.user_id AS userId, fee.fee_amount AS feeAmount, fee.subsidy_category AS subsidyCategory",
            "FROM (",
            "  SELECT c.user_id, c.fee_amount, NULL AS subsidy_category, a.publish_date AS fee_date, 1 AS work_order, c.id AS contributor_id",
            "  FROM biz_work_contributor c JOIN biz_editorial_article a ON c.work_type='EDITORIAL' AND c.work_id=a.id",
            "  WHERE a.is_valid=1 AND a.audit_status='APPROVED' AND a.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, p.publish_date, 2, c.id",
            "  FROM biz_work_contributor c JOIN biz_photography_work p ON c.work_type='PHOTO' AND c.work_id=p.id",
            "  WHERE p.is_valid=1 AND p.audit_status='APPROVED' AND p.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, v.publish_date, 3, c.id",
            "  FROM biz_work_contributor c JOIN biz_visual_work v ON c.work_type='VISUAL' AND c.work_id=v.id",
            "  WHERE v.is_valid=1 AND v.audit_status='APPROVED' AND v.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, w.publish_date, 4, c.id",
            "  FROM biz_work_contributor c JOIN biz_video_work w ON c.work_type='VIDEO' AND c.work_id=w.id",
            "  WHERE w.is_valid=1 AND w.audit_status='APPROVED' AND w.publish_date BETWEEN #{startDate} AND #{endDate}",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, s.category, STR_TO_DATE(CONCAT(s.subsidy_month, '-01'), '%Y-%m-%d'), 5, c.id",
            "  FROM biz_work_contributor c JOIN biz_monthly_subsidy s ON c.work_type='SUBSIDY' AND c.work_id=s.id",
            "  WHERE s.is_valid=1 AND s.audit_status='APPROVED' AND s.subsidy_month=#{month}",
            ") fee",
            "WHERE fee.fee_amount IS NOT NULL AND fee.fee_amount &gt; 0",
            "ORDER BY fee.fee_date, fee.work_order, fee.contributor_id",
            "</script>"
    })
    List<MonthlyFeeComponentVO> selectMonthlyComponents(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("month") String month);

    // 202608121123 - 新增了主席团采编负责人按微信、院网、校报职责排序所需的平台信息查询。
    @Select({
            "SELECT DISTINCT c.user_id AS userId, a.platform",
            "FROM biz_work_contributor c",
            "JOIN biz_editorial_article a ON c.work_type='EDITORIAL' AND c.work_id=a.id",
            "WHERE a.is_valid=1"
    })
    List<MonthlyEditorialPlatformVO> selectEditorialPlatforms();
}
