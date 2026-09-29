package com.sja.system.dal.mysql;

import com.sja.system.application.vo.fee.MonthlyFeeComponentVO;
import com.sja.system.application.vo.fee.MonthlyEditorialPlatformVO;
import com.sja.system.application.vo.fee.MonthlyFeeParticipantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyFeeExportMapper {

    // 202609281642 - 修复了未发布作品不受所选月份范围约束的问题，并带出预览所需的任务标识。
    @Select({
            "<script>",
            "SELECT fee.user_id AS userId, fee.fee_amount AS feeAmount, fee.subsidy_category AS subsidyCategory,",
            "fee.contributor_id AS contributorId, fee.work_type AS workType, fee.work_id AS workId,",
            "fee.title AS title, fee.publish_date AS publishDate, fee.is_published AS isPublished, fee.subsidy_month AS subsidyMonth",
            "FROM (",
            "  SELECT c.user_id, c.fee_amount, NULL AS subsidy_category,",
            "    IF(a.is_published=0, DATE(a.create_time), a.publish_date) AS fee_date, 1 AS work_order, c.id AS contributor_id,",
            "    c.work_type, c.work_id, a.title, a.publish_date, a.is_published, NULL AS subsidy_month",
            "  FROM biz_work_contributor c JOIN biz_editorial_article a ON c.work_type='EDITORIAL' AND c.work_id=a.id",
            "  WHERE a.is_valid=1 AND a.audit_status='APPROVED' AND ((a.is_published=0 AND DATE(a.create_time) BETWEEN #{startDate} AND #{endDate}) OR (a.is_published=1 AND a.publish_date BETWEEN #{startDate} AND #{endDate}))",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, IF(p.is_published=0, DATE(p.create_time), p.publish_date), 2, c.id,",
            "    c.work_type, c.work_id, p.title, p.publish_date, p.is_published, NULL",
            "  FROM biz_work_contributor c JOIN biz_photography_work p ON c.work_type='PHOTO' AND c.work_id=p.id",
            "  WHERE p.is_valid=1 AND p.audit_status='APPROVED' AND ((p.is_published=0 AND DATE(p.create_time) BETWEEN #{startDate} AND #{endDate}) OR (p.is_published=1 AND p.publish_date BETWEEN #{startDate} AND #{endDate}))",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, IF(v.is_published=0, DATE(v.create_time), v.publish_date), 3, c.id,",
            "    c.work_type, c.work_id, v.title, v.publish_date, v.is_published, NULL",
            "  FROM biz_work_contributor c JOIN biz_visual_work v ON c.work_type='VISUAL' AND c.work_id=v.id",
            "  WHERE v.is_valid=1 AND v.audit_status='APPROVED' AND ((v.is_published=0 AND DATE(v.create_time) BETWEEN #{startDate} AND #{endDate}) OR (v.is_published=1 AND v.publish_date BETWEEN #{startDate} AND #{endDate}))",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, NULL, IF(w.is_published=0, DATE(w.create_time), w.publish_date), 4, c.id,",
            "    c.work_type, c.work_id, w.video_title, w.publish_date, w.is_published, NULL",
            "  FROM biz_work_contributor c JOIN biz_video_work w ON c.work_type='VIDEO' AND c.work_id=w.id",
            "  WHERE w.is_valid=1 AND w.audit_status='APPROVED' AND ((w.is_published=0 AND DATE(w.create_time) BETWEEN #{startDate} AND #{endDate}) OR (w.is_published=1 AND w.publish_date BETWEEN #{startDate} AND #{endDate}))",
            "  UNION ALL",
            "  SELECT c.user_id, c.fee_amount, s.category, STR_TO_DATE(CONCAT(s.subsidy_month, '-01'), '%Y-%m-%d'), 5, c.id,",
            "    c.work_type, c.work_id, s.title, NULL, 0, s.subsidy_month",
            "  FROM biz_work_contributor c JOIN biz_monthly_subsidy s ON c.work_type='SUBSIDY' AND c.work_id=s.id",
            "  WHERE s.is_valid=1 AND s.audit_status='APPROVED' AND s.subsidy_month BETWEEN #{startMonth} AND #{endMonth}",
            ") fee",
            "WHERE fee.fee_amount IS NOT NULL AND fee.fee_amount &gt; 0",
            "ORDER BY fee.fee_date, fee.work_order, fee.contributor_id",
            "</script>"
    })
    List<MonthlyFeeComponentVO> selectMonthlyComponents(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("startMonth") String startMonth,
                                                         @Param("endMonth") String endMonth);

    /** 批量查询本次清单中所有任务的参与人员，避免逐笔预览触发 N+1 查询。 */
    @Select({
            "<script>",
            "SELECT c.work_type AS workType, c.work_id AS workId, COALESCE(u.real_name, '未知人员') AS name,",
            "c.role_in_work AS role, c.fee_amount AS feeAmount",
            "FROM biz_work_contributor c LEFT JOIN sys_user u ON u.id=c.user_id",
            "WHERE (c.work_type, c.work_id) IN",
            "<foreach collection='refs' item='ref' open='(' separator=',' close=')'>(#{ref.workType}, #{ref.workId})</foreach>",
            "ORDER BY c.work_type, c.work_id, c.id",
            "</script>"
    })
    List<MonthlyFeeParticipantVO> selectParticipants(@Param("refs") List<WorkRef> refs);

    /** 任务复合主键，用作安全的 MyBatis 参数绑定。 */
    record WorkRef(String workType, Long workId) { }

    // 202608121123 - 新增了主席团采编负责人按微信、院网、校报职责排序所需的平台信息查询。
    @Select({
            "SELECT DISTINCT c.user_id AS userId, a.platform",
            "FROM biz_work_contributor c",
            "JOIN biz_editorial_article a ON c.work_type='EDITORIAL' AND c.work_id=a.id",
            "WHERE a.is_valid=1"
    })
    List<MonthlyEditorialPlatformVO> selectEditorialPlatforms();
}
