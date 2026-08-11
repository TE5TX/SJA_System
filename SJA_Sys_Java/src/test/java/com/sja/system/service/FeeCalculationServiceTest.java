package com.sja.system.service;

import com.sja.system.dto.FeeSubmitDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeeCalculationServiceTest {

    private final FeeCalculationService service = new FeeCalculationService();

    @Test
    void calculatesEditorialWechatByOriginalityAndReads() {
        Map<String, Object> original = info("微信", "原创类");
        original.put("readCount", 10001);
        assertFees(service.calculateAutomaticFees("EDITORIAL", original, authors(2)), "80.0", "80.0");

        Map<String, Object> halfOriginal = info("微信", "半原创类");
        halfOriginal.put("readCount", 20000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", halfOriginal, authors(1)), "20.0");
    }

    @Test
    void calculatesEditorialWebsitePerArticleAndSplitsAuthors() {
        assertFees(service.calculateAutomaticFees("EDITORIAL", info("院网", "莲峰群英"), authors(2)), "60.0", "60.0");
    }

    @Test
    void calculatesEditorialNewspaperByWholeThousandWordTiersAndSplitsAuthors() {
        Map<String, Object> info = info("校报", "原创一般稿件");
        info.put("wordCount", 1500);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(2)), "40.0", "40.0");

        info.put("wordCount", 1999);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "80.0");

        info.put("wordCount", 2000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.0");

        info.put("wordCount", 2073);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.0");

        info.put("wordCount", 2172);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.0");

        info.put("wordCount", 3000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "240.0");
    }

    @Test
    void usesConfiguredFeeStandardDuringCalculation() {
        FeeStandardService standardService = mock(FeeStandardService.class);
        when(standardService.value("EDITORIAL", "校报", "原创一般稿件",
                "PER_1000_WORDS_FEE", new BigDecimal("80")))
                .thenReturn(new BigDecimal("81"));
        FeeCalculationService configuredService = new FeeCalculationService(standardService);
        Map<String, Object> info = info("校报", "原创一般稿件");
        info.put("wordCount", 1000);

        assertFees(configuredService.calculateAutomaticFees("EDITORIAL", info, authors(1)), "81.0");
    }

    @Test
    void roundsEqualSharesUpInsteadOfAssigningRemainderToOnePerson() {
        Map<String, Object> info = info("校报", "原创头版头条");
        info.put("wordCount", 1000);
        // 202608111638 - 修复了三人平均分配100元时出现33.3、33.3、33.4的问题。
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(3)), "33.4", "33.4", "33.4");

        info.put("category", "重新编辑类稿件");
        info.put("wordCount", 8000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(3)), "133.4", "133.4", "133.4");
    }

    @Test
    void calculatesPhotographyWechatFromReadsOnly() {
        Map<String, Object> info = info("微信", "任意展示类别");
        info.put("readCount", 7000);
        assertFees(service.calculateAutomaticFees("PHOTO", info, authors(2)), "50.0", "50.0");
    }

    @Test
    void calculatesPhotographyWebsitePerAuthorImageCount() {
        List<FeeSubmitDTO.ContributorDTO> contributors = authors(6);
        contributors.get(0).setWorkCount(1);
        contributors.get(1).setWorkCount(2);
        contributors.get(2).setWorkCount(3);
        contributors.get(3).setWorkCount(4);
        contributors.get(4).setWorkCount(5);
        contributors.get(5).setWorkCount(6);
        assertFees(service.calculateAutomaticFees("PHOTO", info("院网", ""), contributors),
                "30.0", "30.0", "40.0", "40.0", "60.0", "60.0");
    }

    @Test
    void usesConfiguredPhotographyWebsiteTiers() {
        FeeStandardService standardService = mock(FeeStandardService.class);
        when(standardService.value("PHOTO", "院网", "", "COUNT_UP_TO_2_FEE", new BigDecimal("30")))
                .thenReturn(new BigDecimal("31"));
        when(standardService.value("PHOTO", "院网", "", "COUNT_3_TO_4_FEE", new BigDecimal("40")))
                .thenReturn(new BigDecimal("41"));
        when(standardService.value("PHOTO", "院网", "", "COUNT_5_PLUS_FEE", new BigDecimal("60")))
                .thenReturn(new BigDecimal("61"));
        FeeCalculationService configuredService = new FeeCalculationService(standardService);
        List<FeeSubmitDTO.ContributorDTO> contributors = authors(3);
        contributors.get(0).setWorkCount(2);
        contributors.get(1).setWorkCount(4);
        contributors.get(2).setWorkCount(5);

        assertFees(configuredService.calculateAutomaticFees("PHOTO", info("院网", ""), contributors),
                "31.0", "41.0", "61.0");
    }

    @Test
    void calculatesPhotographyNewspaperPerAuthorImageCount() {
        List<FeeSubmitDTO.ContributorDTO> contributors = authors(3);
        contributors.get(0).setWorkCount(1);
        contributors.get(1).setWorkCount(2);
        contributors.get(2).setWorkCount(6);
        assertFees(service.calculateAutomaticFees("PHOTO", info("校报", ""), contributors), "20.0", "30.0", "40.0");
    }

    @Test
    void calculatesVideoBonusesAndRoleSplit() {
        Map<String, Object> info = info("", "活动新闻类视频");
        info.put("likeCount", 500);
        info.put("isReposted", 1);
        List<FeeSubmitDTO.ContributorDTO> contributors = Arrays.asList(contributor("SHOOTER"), contributor("EDITOR"));
        assertFees(service.calculateAutomaticFees("VIDEO", info, contributors), "455.0", "195.0");
    }

    @Test
    void appliesVideoSpecialMultiplierToBaseFeeButNotLikeBonus() {
        Map<String, Object> info = info("", "活动新闻类视频");
        info.put("likeCount", 639);
        info.put("isSpecialNode", 1);
        info.put("specialMultiplier", 2);
        List<FeeSubmitDTO.ContributorDTO> contributors = Arrays.asList(
                contributor("EDITOR"), contributor("SHOOTER"), contributor("SHOOTER"));

        // 202608111609 - 修复了活动新闻类视频点赞奖励被特殊节点倍数重复翻倍的问题。
        assertFees(service.calculateAutomaticFees("VIDEO", info, contributors), "300.0", "350.0", "350.0");
    }

    @Test
    void addsComplexEditingBonusToShortVideoEditors() {
        Map<String, Object> info = info("", "原创类短视频");
        info.put("isComplexEdit", 1);
        List<FeeSubmitDTO.ContributorDTO> contributors = Arrays.asList(contributor("SHOOTER"), contributor("EDITOR"));
        assertFees(service.calculateAutomaticFees("VIDEO", info, contributors), "200.0", "220.0");
    }

    @Test
    void allowsVideoWithShootersOnly() {
        Map<String, Object> info = info("", "活动新闻类视频");
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("SHOOTER"))), "400.0");
    }

    @Test
    void allowsVideoWithEditorsOnly() {
        Map<String, Object> info = info("", "活动新闻类视频");
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("EDITOR"))), "400.0");
    }

    @Test
    void assignsComplexEditingBonusToShootersWhenNoEditorsExist() {
        Map<String, Object> info = info("", "原创类短视频");
        info.put("isComplexEdit", 1);
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("SHOOTER"))), "420.0");
    }

    @Test
    void calculatesVisualLayoutAndHomepageImages() {
        assertFees(service.calculateAutomaticFees("VISUAL", info("", "普通消息类"), authors(2)), "15.0", "15.0");

        List<FeeSubmitDTO.ContributorDTO> designers = authors(2);
        designers.get(0).setWorkCount(1);
        designers.get(1).setWorkCount(3);
        assertFees(service.calculateAutomaticFees("VISUAL", info("", "校园网首页大图"), designers), "40.0", "120.0");
    }

    @Test
    void acceptsBothVisualSvgCategoryNames() {
        assertFees(service.calculateAutomaticFees("VISUAL",
                info("", "互动、长图、SVG动图、手绘"), authors(1)), "50.0");
        assertFees(service.calculateAutomaticFees("VISUAL",
                info("", "互动、长图、SVG、手绘"), authors(1)), "50.0");
    }

    @Test
    void appliesSpecialNodeMultiplier() {
        Map<String, Object> info = info("微信", "原创类");
        info.put("readCount", 0);
        info.put("isSpecialNode", 1);
        info.put("specialMultiplier", 3);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "90.0");
    }

    @Test
    void calculatesMonthlySubsidiesPerPerson() {
        assertFees(service.calculateAutomaticFees("SUBSIDY", info("", "主席团成员"), authors(2)), "150.0", "150.0");
        assertFees(service.calculateAutomaticFees("SUBSIDY", info("", "校园巡查组员"), authors(1)), "50.0");
    }

    private void assertFees(List<BigDecimal> actual, String... expected) {
        assertEquals(Arrays.stream(expected).map(BigDecimal::new).collect(Collectors.toList()), actual);
    }

    private Map<String, Object> info(String platform, String category) {
        Map<String, Object> info = new HashMap<>();
        info.put("platform", platform);
        info.put("category", category);
        return info;
    }

    private List<FeeSubmitDTO.ContributorDTO> authors(int count) {
        FeeSubmitDTO.ContributorDTO[] contributors = new FeeSubmitDTO.ContributorDTO[count];
        for (int index = 0; index < count; index++) {
            contributors[index] = contributor("AUTHOR");
            contributors[index].setUserId((long) index + 1);
        }
        return Arrays.asList(contributors);
    }

    private FeeSubmitDTO.ContributorDTO contributor(String role) {
        FeeSubmitDTO.ContributorDTO contributor = new FeeSubmitDTO.ContributorDTO();
        contributor.setUserId(1L);
        contributor.setRoleInWork(role);
        return contributor;
    }
}
