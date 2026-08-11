package com.sja.system.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.MonthlyFeeExportDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.MonthlyFeeExportMapper;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.vo.MonthlyFeeComponentVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private static final Set<Integer> MEMBER_ROLE_CODES = new HashSet<>(Arrays.asList(
            1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14));

    private final MonthlyFeeExportMapper monthlyFeeExportMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final OperationLogService operationLogService;

    public void export(MonthlyFeeExportDTO dto, Long operatorId, Integer roleCode,
                       String operatorName, HttpServletResponse response) {
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            throw ApiException.forbidden("只有管理员或秘书长可以导出月份稿费发放名单");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(dto.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw ApiException.badRequest("导出月份格式必须为yyyy-MM");
        }

        List<MonthlyFeeComponentVO> components = monthlyFeeExportMapper.selectMonthlyComponents(
                month.atDay(1), month.atEndOfMonth(), dto.getMonth());
        Map<Long, List<BigDecimal>> amountsByUser = components.stream()
                .collect(Collectors.groupingBy(MonthlyFeeComponentVO::getUserId, LinkedHashMap::new,
                        Collectors.mapping(MonthlyFeeComponentVO::getFeeAmount, Collectors.toList())));

        List<MemberRow> rows = buildMemberRows(amountsByUser);
        writeWorkbook(dto.getMonth(), rows, response);
        operationLogService.log(operatorId, operatorName, String.valueOf(roleCode), "EXPORT",
                "FEE", null, String.format("导出%s月份稿费发放名单，共%d人", dto.getMonth(), rows.size()), "");
    }

    private List<MemberRow> buildMemberRows(Map<Long, List<BigDecimal>> amountsByUser) {
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
            Set<Integer> roles = rolesByUser.getOrDefault(user.getId(), Collections.emptySet());
            Group group = classifyMember(deptsByUser.getOrDefault(user.getId(), Collections.emptyList()), roles);
            if (group == null) continue;
            List<BigDecimal> amounts = amountsByUser.getOrDefault(user.getId(), Collections.emptyList());
            BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            String calculation = amounts.stream().map(this::compactMoney).collect(Collectors.joining("+"));
            rows.add(new MemberRow(group.order, group.name, user.getId(), user.getRealName(),
                    StringUtils.hasText(user.getIdCard()) ? user.getIdCard().trim() : "",
                    calculation, amounts.isEmpty() ? null : total, group.remark));
        }
        rows.sort(Comparator.comparingInt(MemberRow::getGroupOrder).thenComparing(MemberRow::getUserId));
        return rows;
    }

    private Group classifyMember(List<SysDept> depts, Set<Integer> roles) {
        if (roles.contains(1) || roles.contains(3) || depts.stream().anyMatch(item -> "PRESIDIUM".equals(item.getDeptCode()))) {
            return new Group(0, "主席团成员", "主席团");
        }
        if (roles.contains(11) || roles.contains(12)) {
            return new Group(1, "校园巡查小组", roles.contains(11) ? "巡查组长" : "巡查");
        }
        SysDept dept = depts.stream().filter(item -> !"PRESIDIUM".equals(item.getDeptCode())).findFirst().orElse(null);
        if (dept != null) {
            return new Group(10 + (dept.getSortOrder() != null ? dept.getSortOrder() : 90), dept.getDeptName(), "");
        }
        if (roles.stream().anyMatch(MEMBER_ROLE_CODES::contains)) {
            return new Group(99, "其他部员", "");
        }
        return null;
    }

    private void writeWorkbook(String month, List<MemberRow> rows, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("校学生记者团稿费发放名单计算（" + month + "）.xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

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

        // 202608111715 - 新增了按主席团、校园小队和各部门排序的月份稿费发放名单导出。
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
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

    private String compactMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    @Data
    @AllArgsConstructor
    private static class Group {
        private int order;
        private String name;
        private String remark;
    }

    @Data
    @AllArgsConstructor
    private static class MemberRow {
        private int groupOrder;
        private String groupName;
        private Long userId;
        private String realName;
        private String idCard;
        private String calculation;
        private BigDecimal total;
        private String remark;
    }
}
