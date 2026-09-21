package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/** 稿费动态字段的安全读取与公共校验工具。 */
// 202609201644 - 集中五类作品适配器共用的动态字段转换规则。
final class FeeValueReader {

    private FeeValueReader() {
    }

    static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    static Integer integer(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static LocalDate date(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        try {
            return LocalDate.parse(value.toString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static int specialMultiplier(Map<String, Object> info) {
        if (!Objects.equals(integer(info, "isSpecialNode"), 1)) return 1;
        Integer multiplier = integer(info, "specialMultiplier");
        if (multiplier == null || (multiplier != 2 && multiplier != 3)) {
            throw ApiException.badRequest("特殊节点倍数只能选择 2 倍或 3 倍");
        }
        return multiplier;
    }

    static String visualCategory(String category) {
        if ("互动、长图、SVG、手绘".equals(category) || "互动长图SVG手绘".equals(category)) {
            return "互动、长图、SVG动图、手绘";
        }
        return category;
    }

    static <T> void applyIdentity(QueryWrapper<T> query, String linkColumn, String link,
                                  String titleColumn, String title, String categoryColumn,
                                  String category, String dateColumn, LocalDate publishDate) {
        if (StringUtils.hasText(link)) {
            query.eq(linkColumn, link.trim());
            return;
        }
        query.eq(titleColumn, title).eq(categoryColumn, category);
        if (publishDate == null) query.isNull(dateColumn);
        else query.eq(dateColumn, publishDate);
    }
}
