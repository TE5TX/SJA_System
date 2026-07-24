package com.sja.system.service;

import com.sja.system.dto.FeeSubmitDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FeeCalculationService {

    private static final String EDITORIAL = "EDITORIAL";
    private static final String PHOTO = "PHOTO";
    private static final String VIDEO = "VIDEO";
    private static final String VISUAL = "VISUAL";
    private static final String SUBSIDY = "SUBSIDY";

    /** Returns one calculated amount for each contributor, preserving contributor order. */
    public List<BigDecimal> calculateAutomaticFees(String workType,
                                                   Map<String, Object> workInfo,
                                                   List<FeeSubmitDTO.ContributorDTO> contributors) {
        if (contributors == null || contributors.isEmpty()) {
            throw new RuntimeException("参与人员不能为空");
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
                throw new RuntimeException("不支持的稿费类型: " + workType);
        }
        return applySpecialMultiplier(fees, workInfo);
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
                throw new RuntimeException("不支持的采编发布平台: " + platform);
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
                throw new RuntimeException("不支持的摄影发布平台: " + platform);
        }
        return fees;
    }

    private List<BigDecimal> calculateVideo(Map<String, Object> info,
                                            List<FeeSubmitDTO.ContributorDTO> contributors) {
        int shooterCount = (int) contributors.stream().filter(item -> "SHOOTER".equals(item.getRoleInWork())).count();
        int editorCount = (int) contributors.stream().filter(item -> "EDITOR".equals(item.getRoleInWork())).count();
        if (shooterCount + editorCount != contributors.size()) {
            throw new RuntimeException("视频参与人员角色只能是拍摄或剪辑");
        }

        String category = stringValue(info, "category");
        BigDecimal base;
        BigDecimal shootRatio;
        switch (category) {
            case "活动新闻类视频":
                base = amount("400");
                shootRatio = amount("0.70");
                break;
            case "风景类系列短视频":
                base = amount("150");
                shootRatio = amount("0.50");
                break;
            case "原创类短视频":
                base = amount("400");
                shootRatio = amount("0.50");
                break;
            case "原创类长视频":
                base = amount("600");
                shootRatio = amount("0.60");
                break;
            default:
                throw new RuntimeException("不支持的视频类别: " + category);
        }

        int likeCount = intValue(info, "likeCount");
        if (likeCount < 0) throw new RuntimeException("点赞数不能小于 0");
        BigDecimal likeBonus = amount(String.valueOf(Math.max(0, (likeCount - 200) / 100) * 50));
        BigDecimal repostBonus = isReposted(info) ? amount("100") : BigDecimal.ZERO;
        BigDecimal sharedTotal = base.add(likeBonus).add(repostBonus);
        BigDecimal shootTotal;
        BigDecimal editTotal;
        if (shooterCount > 0 && editorCount > 0) {
            shootTotal = sharedTotal.multiply(shootRatio).setScale(2, RoundingMode.HALF_UP);
            editTotal = sharedTotal.subtract(shootTotal);
        } else if (shooterCount > 0) {
            shootTotal = sharedTotal;
            editTotal = BigDecimal.ZERO;
        } else {
            shootTotal = BigDecimal.ZERO;
            editTotal = sharedTotal;
        }

        if (isShortVideo(category) && intValue(info, "isComplexEdit") == 1) {
            if (editorCount > 0) {
                editTotal = editTotal.add(amount("20"));
            } else {
                shootTotal = shootTotal.add(amount("20"));
            }
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
                perPerson = amount("15");
                break;
            case "原创美文（不含制图）":
                perPerson = amount("20");
                break;
            case "原创美文（含部分制图）":
                perPerson = amount("30");
                break;
            case "互动、长图、SVG动图、手绘":
            case "互动长图SVG手绘":
                perPerson = amount("50");
                break;
            case "校园网首页大图":
                List<BigDecimal> fees = new ArrayList<>();
                contributors.forEach(item -> fees.add(amount("40").multiply(BigDecimal.valueOf(requireWorkCount(item)))));
                return fees;
            default:
                throw new RuntimeException("不支持的视觉作品类别: " + category);
        }
        return repeated(contributors.size(), perPerson);
    }

    private List<BigDecimal> calculateSubsidy(Map<String, Object> info, int contributorCount) {
        String category = stringValue(info, "category");
        BigDecimal amount;
        switch (category) {
            case "微信统筹编辑":
            case "排版编辑":
                amount = amount("80");
                break;
            case "校对":
                amount = amount("10");
                break;
            case "主席团成员":
                amount = amount("150");
                break;
            case "校园巡查组长":
                amount = amount("80");
                break;
            case "校园巡查组员":
                amount = amount("50");
                break;
            default:
                throw new RuntimeException("不支持的月度补贴类型: " + category);
        }
        return repeated(contributorCount, amount);
    }

    private BigDecimal calculateEditorialWechat(String category, int readCount) {
        if (readCount < 0) throw new RuntimeException("阅读量不能小于 0");
        if ("半原创类".equals(category) || "非原创类".equals(category)) return amount("20");
        if (!"原创类".equals(category)) throw new RuntimeException("不支持的微信文章类别: " + category);
        return wechatOriginalAmount(readCount);
    }

    private BigDecimal calculateEditorialWebsite(String category) {
        if ("莲峰群英".equals(category)) return amount("120");
        if ("学校新闻".equals(category) || "学校新闻（含学校要闻、校园时讯）".equals(category)) return amount("50");
        throw new RuntimeException("不支持的院网文章类别: " + category);
    }

    private BigDecimal calculateEditorialNewspaper(String category, int wordCount) {
        if (wordCount <= 0) throw new RuntimeException("校报文章字数必须大于 0");
        BigDecimal rate;
        switch (category) {
            case "原创头版头条": rate = amount("100"); break;
            case "原创一般稿件": rate = amount("80"); break;
            case "重新编辑类稿件": rate = amount("50"); break;
            default: throw new RuntimeException("不支持的校报文章类别: " + category);
        }
        return rate.multiply(BigDecimal.valueOf(wordCount)).divide(amount("1000"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePhotographyWechat(int readCount) {
        if (readCount < 0) throw new RuntimeException("阅读量不能小于 0");
        return wechatOriginalAmount(readCount);
    }

    private BigDecimal wechatOriginalAmount(int readCount) {
        if (readCount > 10000) return amount("80");
        if (readCount > 6000) return amount("50");
        return amount("30");
    }

    private BigDecimal calculatePhotographyWebsite(int workCount) {
        if (workCount < 3) return amount("30");
        if (workCount <= 5) return amount("40");
        return amount("60");
    }

    private BigDecimal calculatePhotographyNewspaper(int workCount) {
        if (workCount < 2) return amount("20");
        if (workCount <= 5) return amount("30");
        return amount("40");
    }

    private List<BigDecimal> applySpecialMultiplier(List<BigDecimal> fees, Map<String, Object> info) {
        if (intValue(info, "isSpecialNode") != 1) return scaled(fees);
        int multiplier = intValue(info, "specialMultiplier");
        if (multiplier != 2 && multiplier != 3) {
            throw new RuntimeException("特殊节点倍数只能选择 2 倍或 3 倍");
        }
        List<BigDecimal> result = new ArrayList<>();
        fees.forEach(fee -> result.add(fee.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP)));
        return result;
    }

    private List<BigDecimal> scaled(List<BigDecimal> fees) {
        List<BigDecimal> result = new ArrayList<>();
        fees.forEach(fee -> result.add(fee.setScale(2, RoundingMode.HALF_UP)));
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
            throw new RuntimeException("作品数量必须大于 0");
        }
        return contributor.getWorkCount();
    }

    private List<BigDecimal> repeated(int count, BigDecimal value) {
        return new ArrayList<>(Collections.nCopies(count, value.setScale(2, RoundingMode.HALF_UP)));
    }

    private List<BigDecimal> splitEvenly(BigDecimal total, int count) {
        BigDecimal normalizedTotal = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal regularAmount = normalizedTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        List<BigDecimal> result = new ArrayList<>();
        for (int index = 0; index < count - 1; index++) result.add(regularAmount);
        result.add(normalizedTotal.subtract(regularAmount.multiply(BigDecimal.valueOf(count - 1))));
        return result;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
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
