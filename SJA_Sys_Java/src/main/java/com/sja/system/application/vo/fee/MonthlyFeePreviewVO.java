package com.sja.system.application.vo.fee;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 月份稿费清单的预览结果。行顺序、列值与导出的工作簿保持一致。
 */
@Data
// 202609281642 - 完成了月份稿费清单预览模型及逐笔任务溯源信息。
public class MonthlyFeePreviewVO {
    private String startMonth;
    private String endMonth;
    private String fingerprint;
    private List<Row> rows;
    private BigDecimal grandTotal;

    @Data
    public static class Row {
        private String department;
        private String realName;
        private String idCard;
        private String calculation;
        private BigDecimal total;
        private String remark;
        private List<Term> terms;
    }

    @Data
    public static class Term {
        private Long contributorId;
        private String workType;
        private Long workId;
        private String title;
        private String amountText;
        private BigDecimal amount;
        private LocalDate publishDate;
        private boolean published;
        private String subsidyMonth;
        private List<Participant> participants;
        /** 202609281715 - 全部参与人的实际保存金额之和。 */
        private BigDecimal workTotal;
    }

    @Data
    public static class Participant {
        private String name;
        private String role;
        private BigDecimal amount;
    }
}
