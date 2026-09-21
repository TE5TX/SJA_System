package com.sja.system.mapper.provider;

import com.sja.system.mapper.model.FeeListQuery;

import java.util.Map;

/**
 * 构造五类稿费任务的统一只读 SQL。
 *
 * <p>表名、列名和作品类型均为代码内固定常量；用户输入始终通过 MyBatis 参数绑定，
 * 不参与 SQL 字符串拼接。</p>
 */
public class FeeQuerySqlProvider {

    public String countFeeItems(Map<String, Object> parameters) {
        FeeListQuery query = queryOf(parameters);
        return "SELECT COUNT(1) FROM (" + buildUnion(query) + ") fee_work" + buildSelectedTaskWhere(query);
    }

    public String selectFeeItems(Map<String, Object> parameters) {
        FeeListQuery query = queryOf(parameters);
        return "SELECT id, work_type, title, platform, publish_date, total_amount, audit_status, "
                + "is_published, create_time, submitter_id FROM (" + buildUnion(query) + ") fee_work"
                + buildSelectedTaskWhere(query)
                + " ORDER BY publish_date DESC, create_time DESC, id DESC, "
                + "CASE work_type WHEN 'EDITORIAL' THEN 1 WHEN 'PHOTO' THEN 2 WHEN 'VIDEO' THEN 3 "
                + "WHEN 'VISUAL' THEN 4 WHEN 'SUBSIDY' THEN 5 ELSE 6 END"
                + " LIMIT #{offset}, #{limit}";
    }

    private FeeListQuery queryOf(Map<String, Object> parameters) {
        return (FeeListQuery) parameters.get("query");
    }

    private String buildUnion(FeeListQuery query) {
        StringBuilder sql = new StringBuilder();
        appendIfAllowed(sql, query, "EDITORIAL", "biz_editorial_article", "a.title", "a.platform",
                "a.publish_date", "a.is_published", false);
        appendIfAllowed(sql, query, "PHOTO", "biz_photography_work", "a.title", "a.platform",
                "a.publish_date", "a.is_published", false);
        appendIfAllowed(sql, query, "VIDEO", "biz_video_work", "a.video_title", "NULL",
                "a.publish_date", "a.is_published", false);
        appendIfAllowed(sql, query, "VISUAL", "biz_visual_work", "a.title", "NULL",
                "a.publish_date", "a.is_published", false);
        appendIfAllowed(sql, query, "SUBSIDY", "biz_monthly_subsidy", "a.title", "NULL",
                "NULL", "NULL", true);
        if (sql.length() == 0) {
            return "SELECT NULL AS id, NULL AS work_type, NULL AS title, NULL AS platform, "
                    + "NULL AS publish_date, NULL AS total_amount, NULL AS audit_status, "
                    + "NULL AS is_published, NULL AS create_time, NULL AS submitter_id WHERE 1 = 0";
        }
        sql.setLength(sql.length() - " UNION ALL ".length());
        return sql.toString();
    }

    private void appendIfAllowed(StringBuilder sql, FeeListQuery query, String workType, String table,
                                 String titleColumn, String platformColumn, String publishDateColumn,
                                 String publishedColumn, boolean subsidy) {
        if (query.getWorkTypes() == null || !query.getWorkTypes().contains(workType)) {
            return;
        }
        sql.append("SELECT a.id, '").append(workType).append("' AS work_type, ")
                .append(titleColumn).append(" AS title, ").append(platformColumn).append(" AS platform, ")
                .append(publishDateColumn).append(" AS publish_date, a.total_amount, a.audit_status, ")
                .append(publishedColumn).append(" AS is_published, a.create_time, a.submitter_id ")
                .append("FROM ").append(table).append(" a WHERE a.is_valid = 1");
        appendCommonFilters(sql, query, workType, subsidy);
        if (!subsidy && query.getPlatform() != null) {
            sql.append(" AND a.platform = #{query.platform}");
        }
        if (subsidy) {
            if (query.getMonth() != null) sql.append(" AND a.subsidy_month = #{query.month}");
            if (query.getPublishStartMonth() != null) sql.append(" AND a.subsidy_month >= #{query.publishStartMonth}");
            if (query.getPublishEndMonth() != null) sql.append(" AND a.subsidy_month <= #{query.publishEndMonth}");
        } else {
            if (query.getMonthStart() != null) sql.append(" AND a.publish_date >= #{query.monthStart}");
            if (query.getMonthEnd() != null) sql.append(" AND a.publish_date <= #{query.monthEnd}");
            if (query.getPublishStart() != null) sql.append(" AND a.publish_date >= #{query.publishStart}");
            if (query.getPublishEnd() != null) sql.append(" AND a.publish_date <= #{query.publishEnd}");
            if ("UNPUBLISHED".equals(query.getStatus())) sql.append(" AND a.is_published = 0");
        }
        sql.append(" UNION ALL ");
    }

    private void appendCommonFilters(StringBuilder sql, FeeListQuery query, String workType, boolean subsidy) {
        if (query.getCreateStart() != null) sql.append(" AND a.create_time >= #{query.createStart}");
        if (query.getCreateEndExclusive() != null) sql.append(" AND a.create_time < #{query.createEndExclusive}");
        if (query.getStatus() != null && !query.getStatus().isBlank() && !"UNPUBLISHED".equals(query.getStatus())) {
            sql.append(" AND COALESCE(a.audit_status, 'SUBMITTED') = #{query.status}");
        }
        if (query.getContributorName() != null && !query.getContributorName().isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM biz_work_contributor c ")
                    .append("INNER JOIN sys_user u ON u.id = c.user_id ")
                    .append("WHERE c.work_type = '").append(workType).append("' AND c.work_id = a.id ")
                    .append("AND u.real_name LIKE CONCAT('%', #{query.contributorName}, '%'))");
        }
    }

    private String buildSelectedTaskWhere(FeeListQuery query) {
        if (query.getSelectedTasks() == null || query.getSelectedTasks().isEmpty()) {
            return "";
        }
        StringBuilder sql = new StringBuilder(" WHERE (");
        for (int index = 0; index < query.getSelectedTasks().size(); index++) {
            if (index > 0) sql.append(" OR ");
            sql.append("(work_type = #{query.selectedTasks[").append(index).append("].workType}")
                    .append(" AND id = #{query.selectedTasks[").append(index).append("].id})");
        }
        return sql.append(")").toString();
    }
}
