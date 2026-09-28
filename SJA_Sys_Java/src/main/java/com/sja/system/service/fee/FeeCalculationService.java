package com.sja.system.service.fee;

import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FeeCalculationService {

    // 202608101716 - 将稿费计算统一为保留一位小数并按四舍五入处理。
    private static final int MONEY_SCALE = 1;

    private static final String EDITORIAL = "EDITORIAL";
    private static final String PHOTO = "PHOTO";
    private static final String VIDEO = "VIDEO";
    private static final String VISUAL = "VISUAL";
    private static final String SUBSIDY = "SUBSIDY";

    private final FeeStandardService feeStandardService;

    public FeeCalculationService() {
        this.feeStandardService = null;
    }

    @Autowired
    public FeeCalculationService(FeeStandardService feeStandardService) {
        this.feeStandardService = feeStandardService;
    }

    /** Returns one calculated amount for each contributor, preserving contributor order. */
    public List<BigDecimal> calculateAutomaticFees(String workType,
                                                   Map<String, Object> workInfo,
                                                   List<FeeSubmitDTO.ContributorDTO> contributors) {
        if (contributors == null || contributors.isEmpty()) {
            throw ApiException.badRequest("参与人员不能为空");
        }

        List<BigDecimal> fees;
        switch (workType) {
            case EDITORIAL:
                fees = calculateEditorial(workInfo, contributors.size());
                break;
            case PHOTO:
                fees = calculatePhotography(workInfo, contributors);
                break;
            case VIDEO:
                fees = calculateVideo(workInfo, contributors);
                break;
            case VISUAL:
                fees = calculateVisual(workInfo, contributors);
                break;
            case SUBSIDY:
                fees = calculateSubsidy(workInfo, contributors.size());
                break;
            default:
                throw ApiException.badRequest("不支持的稿费类型: " + workType);
        }
        // 视频的特殊节点倍数已在基础稿费阶段应用，避免点赞等奖励被重复放大。
        return VIDEO.equals(workType) ? scaled(fees) : applySpecialMultiplier(fees, workInfo);
    }

    private List<BigDecimal> calculateEditorial(Map<String, Object> info, int authorCount) {
        String platform = stringValue(info, "platform");
        String category = stringValue(info, "category");
        switch (platform) {
            case "微信":
                return repeated(authorCount, calculateEditorialWechat(category, intValue(info, "readCount")));
            case "院网":
                return splitEvenly(calculateEditorialWebsite(category), authorCount);
            case "校报":
                return splitEvenly(calculateEditorialNewspaper(category, intValue(info, "wordCount")), authorCount);
            default:
                throw ApiException.badRequest("不支持的采编发布平台: " + platform);
        }
    }

    private List<BigDecimal> calculatePhotography(Map<String, Object> info,
                                                   List<FeeSubmitDTO.ContributorDTO> contributors) {
        String platform = stringValue(info, "platform");
        List<BigDecimal> fees = new ArrayList<>();
        switch (platform) {
            case "微信":
                BigDecimal wechatFee = calculatePhotographyWechat(intValue(info, "readCount"));
                contributors.forEach(contributor -> fees.add(wechatFee));
                break;
            case "院网":
                contributors.forEach(contributor -> fees.add(calculatePhotographyWebsite(requireWorkCount(contributor))));
                break;
            case "校报":
                contributors.forEach(contributor -> fees.add(calculatePhotographyNewspaper(requireWorkCount(contributor))));
                break;
            default:
                throw ApiException.badRequest("不支持的摄影发布平台: " + platform);
        }
        return fees;
    }

    private List<BigDecimal> calculateVideo(Map<String, Object> info,
                                            List<FeeSubmitDTO.ContributorDTO> contributors) {
        int shooterCount = (int) contributors.stream().filter(item -> "SHOOTER".equals(item.getRoleInWork())).count();
        int editorCount = (int) contributors.stream().filter(item -> "EDITOR".equals(item.getRoleInWork())).count();
        if (shooterCount + editorCount != contributors.size()) {
            throw ApiException.badRequest("视频参与人员角色只能是拍摄或剪辑");
        }

        String category = stringValue(info, "category");
        BigDecimal base;
        BigDecimal shootRatio;
        boolean splitAcrossAllContributors = false;
        // 202608101042 - 修复了视频部类别和人员角色超出前端业务范围的问题。
        switch (category) {
            case "活动新闻类视频":
                base = rule("VIDEO", "", category, "BASE_FEE", "400");
                shootRatio = rule("VIDEO", "", category, "SHOOT_RATIO", "0.70");
                break;
            case "风景类系列短视频":
                base = rule("VIDEO", "", category, "BASE_FEE", "150");
                shootRatio = BigDecimal.ZERO;
                splitAcrossAllContributors = true;
                break;
            case "原创类短视频":
                base = rule("VIDEO", "", category, "BASE_FEE", "400");
                shootRatio = BigDecimal.ZERO;
                splitAcrossAllContributors = true;
                break;
            case "原创类长视频":
                base = rule("VIDEO", "", category, "BASE_FEE", "600");
                shootRatio = rule("VIDEO", "", category, "SHOOT_RATIO", "0.60");
                break;
            default:
                throw ApiException.badRequest("不支持的视频类别: " + category);
        }

        // 202608111609 - 修复了视频特殊节点将点赞、转载及复杂剪辑奖励一并翻倍的问题。
        base = applySpecialMultiplier(base, info);

        int likeCount = intValue(info, "likeCount");
        if (likeCount < 0) throw ApiException.badRequest("点赞数不能小于 0");
        int likeThreshold = rule("VIDEO", "", "", "LIKE_THRESHOLD", "200").intValue();
        int likeStep = rule("VIDEO", "", "", "LIKE_STEP", "100").intValue();
        BigDecimal likeBonusPerStep = rule("VIDEO", "", "", "LIKE_BONUS", "50");
        BigDecimal likeBonus = likeBonusPerStep.multiply(BigDecimal.valueOf(
                Math.max(0, (likeCount - likeThreshold) / Math.max(1, likeStep))));
        BigDecimal repostBonus = isReposted(info)
                ? rule("VIDEO", "", "", "REPRINT_BONUS", "100") : BigDecimal.ZERO;
        BigDecimal sharedTotal = base.add(likeBonus).add(repostBonus);

        // 202609281105 - 完成了两类短视频基础稿费和公共奖励由全部拍摄、剪辑人员共同均分的规则调整。
        if (splitAcrossAllContributors) {
            List<BigDecimal> fees = splitEvenly(sharedTotal, contributors.size());
            if (isShortVideo(category) && intValue(info, "isComplexEdit") == 1) {
                if (editorCount == 0) {
                    throw ApiException.badRequest("原创类短视频勾选复杂剪辑时必须有剪辑人员");
                }
                // 复杂剪辑奖励只在剪辑人员之间均分，不参与公共稿费的全员均分。
                List<BigDecimal> editBonuses = splitEvenly(
                        rule("VIDEO", "", "", "COMPLEX_EDIT_BONUS", "20"), editorCount);
                int editorIndex = 0;
                for (int index = 0; index < contributors.size(); index++) {
                    if ("EDITOR".equals(contributors.get(index).getRoleInWork())) {
                        fees.set(index, fees.get(index).add(editBonuses.get(editorIndex++)));
                    }
                }
            }
            return fees;
        }

        // 活动新闻和原创长视频仍先按角色比例分配，角色内部沿用向上取整的均分规则。
        BigDecimal shootTotal;
        BigDecimal editTotal;
        if (shooterCount > 0 && editorCount > 0) {
            shootTotal = sharedTotal.multiply(shootRatio).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            editTotal = sharedTotal.subtract(shootTotal);
        } else if (shooterCount > 0) {
            shootTotal = sharedTotal;
            editTotal = BigDecimal.ZERO;
        } else {
            shootTotal = BigDecimal.ZERO;
            editTotal = sharedTotal;
        }

        List<BigDecimal> shooterFees = shooterCount > 0 ? splitEvenly(shootTotal, shooterCount) : Collections.emptyList();
        List<BigDecimal> editorFees = editorCount > 0 ? splitEvenly(editTotal, editorCount) : Collections.emptyList();

        List<BigDecimal> fees = new ArrayList<>();
        int shooterIndex = 0;
        int editorIndex = 0;
        for (FeeSubmitDTO.ContributorDTO contributor : contributors) {
            if ("SHOOTER".equals(contributor.getRoleInWork())) {
                fees.add(shooterFees.get(shooterIndex++));
            } else if ("EDITOR".equals(contributor.getRoleInWork())) {
                fees.add(editorFees.get(editorIndex++));
            }
        }
        return fees;
    }

    private List<BigDecimal> calculateVisual(Map<String, Object> info,
                                             List<FeeSubmitDTO.ContributorDTO> contributors) {
        String category = stringValue(info, "category");
        BigDecimal perPerson;
        switch (category) {
            case "普通消息类":
                perPerson = rule("VISUAL", "微信", category, "PER_PERSON_FEE", "15");
                break;
            case "原创美文（不含制图）":
                perPerson = rule("VISUAL", "微信", category, "PER_PERSON_FEE", "20");
                break;
            case "原创美文（含部分制图）":
                perPerson = rule("VISUAL", "微信", category, "PER_PERSON_FEE", "30");
                break;
            case "互动、长图、SVG动图、手绘":
                // 202608101042 - 修复了历史表格 SVG 同义类别无法按前端 SVG动图 类别计费的问题。
            case "互动、长图、SVG、手绘":
            case "互动长图SVG手绘":
                perPerson = rule("VISUAL", "微信", "互动、长图、SVG动图、手绘", "PER_PERSON_FEE", "50");
                break;
            case "校园网首页大图":
                List<BigDecimal> fees = new ArrayList<>();
                BigDecimal perImage = rule("VISUAL", "校园网", category, "PER_IMAGE_FEE", "40");
                contributors.forEach(item -> fees.add(perImage.multiply(BigDecimal.valueOf(requireWorkCount(item)))));
                return fees;
            default:
                throw ApiException.badRequest("不支持的视觉作品类别: " + category);
        }
        return repeated(contributors.size(), perPerson);
    }

    private List<BigDecimal> calculateSubsidy(Map<String, Object> info, int contributorCount) {
        String category = stringValue(info, "category");
        BigDecimal amount;
        switch (category) {
            case "微信统筹编辑":
            case "排版编辑":
                amount = rule("MANAGEMENT", "月度补贴", category, "PER_PERSON_FEE", "80");
                break;
            case "校对":
                amount = rule("MANAGEMENT", "月度补贴", category, "PER_PERSON_FEE", "10");
                break;
            case "主席团成员":
                amount = rule("MANAGEMENT", "月度补贴", category, "PER_PERSON_FEE", "150");
                break;
            case "校园巡查组长":
                amount = rule("MANAGEMENT", "月度补贴", category, "PER_PERSON_FEE", "80");
                break;
            case "校园巡查组员":
                amount = rule("MANAGEMENT", "月度补贴", category, "PER_PERSON_FEE", "50");
                break;
            default:
                throw ApiException.badRequest("不支持的月度补贴类型: " + category);
        }
        return repeated(contributorCount, amount);
    }

    private BigDecimal calculateEditorialWechat(String category, int readCount) {
        if (readCount < 0) throw ApiException.badRequest("阅读量不能小于 0");
        if ("半原创类".equals(category) || "非原创类".equals(category)) {
            return rule("EDITORIAL", "微信", "半原创类", "PER_PERSON_FEE", "20");
        }
        if (!"原创类".equals(category)) throw ApiException.badRequest("不支持的微信文章类别: " + category);
        return wechatOriginalAmount(readCount);
    }

    private BigDecimal calculateEditorialWebsite(String category) {
        if ("莲峰群英".equals(category)) return rule("EDITORIAL", "院网", category, "PER_ARTICLE_FEE", "120");
        if ("学校新闻".equals(category) || "学校新闻（含学校要闻、校园时讯）".equals(category)) {
            return rule("EDITORIAL", "院网", "学校新闻（含学校要闻、校园时讯）", "PER_ARTICLE_FEE", "50");
        }
        throw ApiException.badRequest("不支持的院网文章类别: " + category);
    }

    private BigDecimal calculateEditorialNewspaper(String category, int wordCount) {
        if (wordCount <= 0) throw ApiException.badRequest("校报文章字数必须大于 0");
        BigDecimal rate;
        switch (category) {
            case "原创头版头条": rate = rule("EDITORIAL", "校报", category, "PER_1000_WORDS_FEE", "100"); break;
            case "原创一般稿件": rate = rule("EDITORIAL", "校报", category, "PER_1000_WORDS_FEE", "80"); break;
            case "重新编辑类稿件": rate = rule("EDITORIAL", "校报", category, "PER_1000_WORDS_FEE", "50"); break;
            default: throw ApiException.badRequest("不支持的校报文章类别: " + category);
        }
        // 202608101042 - 修复了校报未按完整千字阶梯计费的问题，2073/2172字计2档，3000字计3档。
        long billingUnits = Math.max(1L, wordCount / 1000L);
        return rate.multiply(BigDecimal.valueOf(billingUnits));
    }

    private BigDecimal calculatePhotographyWechat(int readCount) {
        if (readCount < 0) throw ApiException.badRequest("阅读量不能小于 0");
        return wechatOriginalAmount(readCount);
    }

    private BigDecimal wechatOriginalAmount(int readCount) {
        int highThreshold = rule("WECHAT", "微信", "原创类", "READ_HIGH_THRESHOLD", "10000").intValue();
        int middleThreshold = rule("WECHAT", "微信", "原创类", "READ_MIDDLE_THRESHOLD", "6000").intValue();
        if (readCount > highThreshold) return rule("WECHAT", "微信", "原创类", "READ_HIGH_FEE", "80");
        if (readCount > middleThreshold) return rule("WECHAT", "微信", "原创类", "READ_MIDDLE_FEE", "50");
        return rule("WECHAT", "微信", "原创类", "READ_BASE_FEE", "30");
    }

    private BigDecimal calculatePhotographyWebsite(int workCount) {
        // 202608101635 - 修复了院网摄影选图数量分档及配置键不一致的问题。
        if (workCount <= 2) return rule("PHOTO", "院网", "", "COUNT_UP_TO_2_FEE", "30");
        if (workCount <= 4) return rule("PHOTO", "院网", "", "COUNT_3_TO_4_FEE", "40");
        return rule("PHOTO", "院网", "", "COUNT_5_PLUS_FEE", "60");
    }

    private BigDecimal calculatePhotographyNewspaper(int workCount) {
        if (workCount < 2) return rule("PHOTO", "校报", "", "COUNT_1_FEE", "20");
        if (workCount <= 5) return rule("PHOTO", "校报", "", "COUNT_2_TO_5_FEE", "30");
        return rule("PHOTO", "校报", "", "COUNT_OVER_5_FEE", "40");
    }

    private List<BigDecimal> applySpecialMultiplier(List<BigDecimal> fees, Map<String, Object> info) {
        if (intValue(info, "isSpecialNode") != 1) return scaled(fees);
        int multiplier = specialMultiplier(info);
        List<BigDecimal> result = new ArrayList<>();
        fees.forEach(fee -> result.add(fee.multiply(BigDecimal.valueOf(multiplier)).setScale(MONEY_SCALE, RoundingMode.HALF_UP)));
        return result;
    }

    private BigDecimal applySpecialMultiplier(BigDecimal amount, Map<String, Object> info) {
        if (intValue(info, "isSpecialNode") != 1) return amount;
        return amount.multiply(BigDecimal.valueOf(specialMultiplier(info)));
    }

    private int specialMultiplier(Map<String, Object> info) {
        int multiplier = intValue(info, "specialMultiplier");
        if (multiplier != 2 && multiplier != 3) {
            throw ApiException.badRequest("特殊节点倍数只能选择 2 倍或 3 倍");
        }
        return multiplier;
    }

    private List<BigDecimal> scaled(List<BigDecimal> fees) {
        List<BigDecimal> result = new ArrayList<>();
        fees.forEach(fee -> result.add(fee.setScale(MONEY_SCALE, RoundingMode.HALF_UP)));
        return result;
    }

    private boolean isReposted(Map<String, Object> info) {
        String status = stringValue(info, "reprintStatus");
        return intValue(info, "isReposted") == 1 || (!status.isEmpty() && !"无".equals(status));
    }

    private boolean isShortVideo(String category) {
        return "原创类短视频".equals(category);
    }

    private int requireWorkCount(FeeSubmitDTO.ContributorDTO contributor) {
        if (contributor.getWorkCount() == null || contributor.getWorkCount() <= 0) {
            throw ApiException.badRequest("作品数量必须大于 0");
        }
        return contributor.getWorkCount();
    }

    private List<BigDecimal> repeated(int count, BigDecimal value) {
        return new ArrayList<>(Collections.nCopies(count, value.setScale(MONEY_SCALE, RoundingMode.HALF_UP)));
    }

    private List<BigDecimal> splitEvenly(BigDecimal total, int count) {
        BigDecimal normalizedTotal = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        // 202608111638 - 修复了多人平均分配稿费时最后一人因补差额导致个人金额不一致的问题。
        // 202609281105 - 明确每人保留一位小数并向上取整；作品总额由保存层累加实际个人稿费，不在此处补尾差。
        BigDecimal perPerson = normalizedTotal.divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.CEILING);
        return new ArrayList<>(Collections.nCopies(count, perPerson));
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal rule(String deptCode, String platform, String category,
                            String ruleKey, String fallback) {
        BigDecimal defaultValue = amount(fallback);
        return feeStandardService == null
                ? defaultValue
                : feeStandardService.value(deptCode, platform, category, ruleKey, defaultValue);
    }

    private String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private int intValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().trim().isEmpty()) return 0;
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
