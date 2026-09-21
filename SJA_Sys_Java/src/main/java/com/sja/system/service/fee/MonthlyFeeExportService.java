package com.sja.system.service.fee;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.application.model.FeeExportFile;
import com.sja.system.service.authorization.KnownRole;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.MonthlyFeeExportDTO;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.dataobject.SysUserDept;
import com.sja.system.dal.dataobject.SysUserRole;
import com.sja.system.dal.mysql.MonthlyFeeExportMapper;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserRoleMapper;
import com.sja.system.application.vo.fee.MonthlyFeeComponentVO;
import com.sja.system.application.vo.fee.MonthlyEditorialPlatformVO;
import com.sja.system.service.system.OperationLogService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyFeeExportService {

    private static final int MONEY_SCALE = 1;
    private final MonthlyFeeExportMapper monthlyFeeExportMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final OperationLogService operationLogService;

    /**
     * 生成指定月份区间的稿费发放名单。
     *
     * @return 与HTTP协议无关的Excel文件
     */
    public FeeExportFile export(MonthlyFeeExportDTO dto, Long operatorId, Integer roleCode,
                                String operatorName) {
        if (!RoleAuthorizationPolicy.canManageMonthlyFeeExport(roleCode)) {
            throw ApiException.forbidden("只有管理员或秘书长可以导出月份稿费发放名单");
        }

        YearMonth startMonth;
        YearMonth endMonth;
        try {
            startMonth = YearMonth.parse(dto.getStartMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
            endMonth = YearMonth.parse(dto.getEndMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw ApiException.badRequest("导出月份格式必须为yyyy-MM");
        }
        // 202609100927 - 完成了月份区间顺序校验及包含首尾月份的汇总。
        if (startMonth.isAfter(endMonth)) {
            throw ApiException.badRequest("开始月份不能晚于结束月份");
        }

        List<MonthlyFeeComponentVO> components = monthlyFeeExportMapper.selectMonthlyComponents(
                startMonth.atDay(1), endMonth.atEndOfMonth(), dto.getStartMonth(), dto.getEndMonth());
        Map<Long, List<BigDecimal>> amountsByUser = components.stream()
                .collect(Collectors.groupingBy(MonthlyFeeComponentVO::getUserId, LinkedHashMap::new,
                        Collectors.mapping(MonthlyFeeComponentVO::getFeeAmount, Collectors.toList())));
        Map<Long, Set<String>> subsidyCategoriesByUser = components.stream()
                .filter(item -> StringUtils.hasText(item.getSubsidyCategory()))
                .collect(Collectors.groupingBy(MonthlyFeeComponentVO::getUserId, LinkedHashMap::new,
                        Collectors.mapping(MonthlyFeeComponentVO::getSubsidyCategory, Collectors.toSet())));
        Map<Long, Set<String>> editorialPlatformsByUser = monthlyFeeExportMapper.selectEditorialPlatforms().stream()
                .filter(item -> item.getUserId() != null && StringUtils.hasText(item.getPlatform()))
                .collect(Collectors.groupingBy(MonthlyEditorialPlatformVO::getUserId, LinkedHashMap::new,
                        Collectors.mapping(MonthlyEditorialPlatformVO::getPlatform, Collectors.toSet())));

        List<MemberRow> rows = buildMemberRows(amountsByUser, subsidyCategoriesByUser, editorialPlatformsByUser);
        String monthRange = dto.getStartMonth() + "至" + dto.getEndMonth();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeWorkbook(rows, outputStream);
        operationLogService.log(operatorId, operatorName, String.valueOf(roleCode), "EXPORT",
                "FEE", null, String.format("导出%s稿费发放名单，共%d人", monthRange, rows.size()), "");
        // 202609201130 - 修复了月份稿费导出服务直接操作Servlet响应的问题。
        return new FeeExportFile("校学生记者团稿费发放名单计算（" + monthRange + "）.xlsx",
                FeeExportFile.XLSX_CONTENT_TYPE, outputStream.toByteArray());
    }

    private List<MemberRow> buildMemberRows(Map<Long, List<BigDecimal>> amountsByUser,
                                            Map<Long, Set<String>> subsidyCategoriesByUser,
                                            Map<Long, Set<String>> editorialPlatformsByUser) {
        List<SysDept> depts = sysDeptMapper.selectList(new QueryWrapper<SysDept>()
                .eq("is_valid", 1).orderByAsc("sort_order", "id"));
        Map<Long, SysDept> deptById = depts.stream().collect(Collectors.toMap(SysDept::getId, item -> item));
        Map<Long, List<SysDept>> deptsByUser = new HashMap<>();
        for (SysUserDept relation : sysUserDeptMapper.selectList(new QueryWrapper<>())) {
            SysDept dept = deptById.get(relation.getDeptId());
            if (dept != null) deptsByUser.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(dept);
        }
        deptsByUser.values().forEach(items -> items.sort(Comparator
                .comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDept::getId)));

        Map<Long, SysRole> roleById = sysRoleMapper.selectList(new QueryWrapper<SysRole>().eq("is_valid", 1))
                .stream().collect(Collectors.toMap(SysRole::getId, item -> item));
        Map<Long, Set<Integer>> rolesByUser = new HashMap<>();
        for (SysUserRole relation : sysUserRoleMapper.selectList(new QueryWrapper<>())) {
            SysRole role = roleById.get(relation.getRoleId());
            if (role != null) rolesByUser.computeIfAbsent(relation.getUserId(), key -> new HashSet<>()).add(role.getRoleCode());
        }

        List<MemberRow> rows = new ArrayList<>();
        List<SysUser> users = sysUserMapper.selectList(new QueryWrapper<SysUser>().eq("is_valid", 1).orderByAsc("id"));
        for (SysUser user : users) {
            List<BigDecimal> amounts = amountsByUser.getOrDefault(user.getId(), Collections.emptyList());
            // 202608121137 - 修复了月份稿费名单仍导出当月没有稿费人员的问题。
            if (amounts.isEmpty()) continue;
            Set<Integer> roles = rolesByUser.getOrDefault(user.getId(), Collections.emptySet());
            Set<String> subsidyCategories = subsidyCategoriesByUser.getOrDefault(user.getId(), Collections.emptySet());
            Set<String> editorialPlatforms = editorialPlatformsByUser.getOrDefault(user.getId(), Collections.emptySet());
            Group group = classifyMember(
                    deptsByUser.getOrDefault(user.getId(), Collections.emptyList()), roles,
                    subsidyCategories, editorialPlatforms);
            if (group == null) continue;
            BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            String calculation = amounts.stream().map(this::compactMoney).collect(Collectors.joining("+"));
            rows.add(new MemberRow(group.sectionOrder, group.departmentOrder, group.roleOrder,
                    group.name, user.getId(), user.getRealName(),
                    StringUtils.hasText(user.getIdCard()) ? user.getIdCard().trim() : "",
                    calculation, amounts.isEmpty() ? null : total, group.remark));
        }
        // 202608121041 - 修复了月份稿费名单未按主席团、巡查小队、部员及部门角色顺序排列的问题。
        rows.sort(Comparator.comparingInt(MemberRow::getSectionOrder)
                .thenComparingInt(MemberRow::getDepartmentOrder)
                .thenComparingInt(MemberRow::getRoleOrder)
                .thenComparing(MemberRow::getUserId));
        return rows;
    }

    private Group classifyMember(List<SysDept> depts, Set<Integer> roles, Set<String> subsidyCategories,
                                 Set<String> editorialPlatforms) {
        SysDept ordinaryDept = depts.stream()
                .filter(item -> !"PRESIDIUM".equals(item.getDeptCode()))
                .findFirst().orElse(null);
        int departmentOrder = departmentOrder(ordinaryDept);
        boolean chairman = roles.contains(KnownRole.CHAIRMAN.code())
                || roles.contains(KnownRole.SECRETARY_GENERAL.code())
                || subsidyCategories.contains("主席团成员")
                || depts.stream().anyMatch(item -> "PRESIDIUM".equals(item.getDeptCode()));
        if (chairman) {
            // 202608121123 - 修复了主席团未按主席、秘书长及各部门部长职责顺序排列的问题。
            return new Group(0, 0, chairmanOrder(ordinaryDept, roles, editorialPlatforms),
                    "主席团成员", chairmanRemark(roles, subsidyCategories));
        }
        boolean patrolLeader = roles.contains(KnownRole.PATROL_LEADER.code())
                || subsidyCategories.contains("校园巡查组长");
        boolean patrolMember = roles.contains(KnownRole.PATROL_MEMBER.code())
                || subsidyCategories.contains("校园巡查组员");
        if (patrolLeader || patrolMember) {
            boolean leader = patrolLeader;
            return new Group(1, 0, leader ? 0 : 1, "校园巡查小组", leader ? "巡查组长" : "巡查");
        }
        if (ordinaryDept != null) {
            return new Group(2, departmentOrder, 0, ordinaryDept.getDeptName(), "");
        }
        if (roles.stream().anyMatch(RoleAuthorizationPolicy::isMonthlyMemberRole)) {
            return new Group(2, 99, 0, "其他部员", "");
        }
        return null;
    }

    private int chairmanOrder(SysDept dept, Set<Integer> roles, Set<String> editorialPlatforms) {
        if (roles.contains(KnownRole.CHAIRMAN.code())) return 0;
        if (roles.contains(KnownRole.SECRETARY_GENERAL.code())) return 1;
        if (dept == null) return 8;
        switch (String.valueOf(dept.getDeptCode())) {
            case "EDITORIAL":
                if (roles.contains(KnownRole.WECHAT_EDITOR.code())) return 2;
                return editorialPlatforms.contains("院网") ? 3 : 4;
            case "PHOTOGRAPHY": return 5;
            case "VISUAL": return 6;
            case "VIDEO": return 7;
            default: return 8;
        }
    }

    private int departmentOrder(SysDept dept) {
        if (dept == null) return 99;
        switch (String.valueOf(dept.getDeptCode())) {
            case "EDITORIAL": return 0;
            case "PHOTOGRAPHY": return 1;
            case "VISUAL": return 2;
            case "VIDEO": return 3;
            default: return 90 + (dept.getSortOrder() != null ? dept.getSortOrder() : 9);
        }
    }

    private String chairmanRemark(Set<Integer> roles, Set<String> subsidyCategories) {
        List<String> remarks = new ArrayList<>();
        remarks.add("主席团");
        if (roles.contains(KnownRole.WECHAT_EDITOR.code()) || subsidyCategories.contains("微信统筹编辑")) {
            remarks.add("微信编辑");
        }
        if (roles.contains(KnownRole.LAYOUT_EDITOR.code()) || subsidyCategories.contains("排版编辑")) {
            remarks.add("排版编辑");
        }
        return String.join("+", remarks);
    }

    private void writeWorkbook(List<MemberRow> rows, OutputStream outputStream) {
        List<List<String>> head = Arrays.asList(
                Collections.singletonList("所属部门"), Collections.singletonList("人员姓名"),
                Collections.singletonList("人员身份证号码"), Collections.singletonList("人员稿费计算情况"),
                Collections.singletonList("实发金额"), Collections.singletonList("备注"));
        List<List<Object>> data = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (int index = 0; index < rows.size(); index++) {
            MemberRow row = rows.get(index);
            boolean firstInGroup = index == 0 || !row.getGroupName().equals(rows.get(index - 1).getGroupName());
            data.add(Arrays.asList(firstInGroup ? row.getGroupName() : "", row.getRealName(), row.getIdCard(),
                    row.getCalculation(), row.getTotal(), row.getRemark()));
            if (row.getTotal() != null) grandTotal = grandTotal.add(row.getTotal());
        }
        data.add(Arrays.asList("", "合计", "", "", grandTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP), ""));

        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(headStyle);
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);

        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(contentStyle);

        // 202608121123 - 修复了月份稿费导出表未统一居中且缺少框线的问题。
        try (ExcelWriter writer = EasyExcel.write(outputStream)
                .registerWriteHandler(new HorizontalCellStyleStrategy(headStyle, contentStyle))
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).build()) {
            com.alibaba.excel.write.builder.ExcelWriterSheetBuilder builder = EasyExcel.writerSheet(0, "月份稿费发放名单").head(head);
            int start = 0;
            while (start < rows.size()) {
                int end = start;
                while (end + 1 < rows.size() && rows.get(end + 1).getGroupName().equals(rows.get(start).getGroupName())) end++;
                if (end > start) builder.registerWriteHandler(new OnceAbsoluteMergeStrategy(start + 1, end + 1, 0, 0));
                start = end + 1;
            }
            WriteSheet sheet = builder.build();
            writer.write(data, sheet);
        } catch (Exception e) {
            throw new RuntimeException("月份稿费导出失败: " + e.getMessage(), e);
        }
    }

    private void applyThinBorders(WriteCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String compactMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    @Data
    @AllArgsConstructor
    private static class Group {
        private int sectionOrder;
        private int departmentOrder;
        private int roleOrder;
        private String name;
        private String remark;
    }

    @Data
    @AllArgsConstructor
    private static class MemberRow {
        private int sectionOrder;
        private int departmentOrder;
        private int roleOrder;
        private String groupName;
        private Long userId;
        private String realName;
        private String idCard;
        private String calculation;
        private BigDecimal total;
        private String remark;
    }
}
