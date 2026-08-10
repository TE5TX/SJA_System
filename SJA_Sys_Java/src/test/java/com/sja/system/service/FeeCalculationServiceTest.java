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
        assertFees(service.calculateAutomaticFees("EDITORIAL", original, authors(2)), "80.00", "80.00");

        Map<String, Object> halfOriginal = info("微信", "半原创类");
        halfOriginal.put("readCount", 20000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", halfOriginal, authors(1)), "20.00");
    }

    @Test
    void calculatesEditorialWebsitePerArticleAndSplitsAuthors() {
        assertFees(service.calculateAutomaticFees("EDITORIAL", info("院网", "莲峰群英"), authors(2)), "60.00", "60.00");
    }

    @Test
    void calculatesEditorialNewspaperByWholeThousandWordTiersAndSplitsAuthors() {
        Map<String, Object> info = info("校报", "原创一般稿件");
        info.put("wordCount", 1500);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(2)), "40.00", "40.00");

        info.put("wordCount", 1999);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "80.00");

        info.put("wordCount", 2000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.00");

        info.put("wordCount", 2073);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.00");

        info.put("wordCount", 2172);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "160.00");

        info.put("wordCount", 3000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "240.00");
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

        assertFees(configuredService.calculateAutomaticFees("EDITORIAL", info, authors(1)), "81.00");
    }

    @Test
    void assignsDivisionRemainderWithoutLosingTaskTotal() {
        Map<String, Object> info = info("校报", "原创头版头条");
        info.put("wordCount", 1000);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(3)), "33.33", "33.33", "33.34");
    }

    @Test
    void calculatesPhotographyWechatFromReadsOnly() {
        Map<String, Object> info = info("微信", "任意展示类别");
        info.put("readCount", 7000);
        assertFees(service.calculateAutomaticFees("PHOTO", info, authors(2)), "50.00", "50.00");
    }

    @Test
    void calculatesPhotographyWebsitePerAuthorImageCount() {
        List<FeeSubmitDTO.ContributorDTO> contributors = authors(4);
        contributors.get(0).setWorkCount(1);
        contributors.get(1).setWorkCount(2);
        contributors.get(2).setWorkCount(5);
        contributors.get(3).setWorkCount(6);
        assertFees(service.calculateAutomaticFees("PHOTO", info("院网", ""), contributors),
                "30.00", "40.00", "40.00", "60.00");
    }

    @Test
    void calculatesPhotographyNewspaperPerAuthorImageCount() {
        List<FeeSubmitDTO.ContributorDTO> contributors = authors(3);
        contributors.get(0).setWorkCount(1);
        contributors.get(1).setWorkCount(2);
        contributors.get(2).setWorkCount(6);
        assertFees(service.calculateAutomaticFees("PHOTO", info("校报", ""), contributors), "20.00", "30.00", "40.00");
    }

    @Test
    void calculatesVideoBonusesAndRoleSplit() {
        Map<String, Object> info = info("", "活动新闻类视频");
        info.put("likeCount", 500);
        info.put("isReposted", 1);
        List<FeeSubmitDTO.ContributorDTO> contributors = Arrays.asList(contributor("SHOOTER"), contributor("EDITOR"));
        assertFees(service.calculateAutomaticFees("VIDEO", info, contributors), "455.00", "195.00");
    }

    @Test
    void addsComplexEditingBonusToShortVideoEditors() {
        Map<String, Object> info = info("", "原创类短视频");
        info.put("isComplexEdit", 1);
        List<FeeSubmitDTO.ContributorDTO> contributors = Arrays.asList(contributor("SHOOTER"), contributor("EDITOR"));
        assertFees(service.calculateAutomaticFees("VIDEO", info, contributors), "200.00", "220.00");
    }

    @Test
    void allowsVideoWithShootersOnly() {
        Map<String, Object> info = info("", "活动新闻类视频");
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("SHOOTER"))), "400.00");
    }

    @Test
    void allowsVideoWithEditorsOnly() {
        Map<String, Object> info = info("", "活动新闻类视频");
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("EDITOR"))), "400.00");
    }

    @Test
    void assignsComplexEditingBonusToShootersWhenNoEditorsExist() {
        Map<String, Object> info = info("", "原创类短视频");
        info.put("isComplexEdit", 1);
        assertFees(service.calculateAutomaticFees("VIDEO", info, Arrays.asList(contributor("SHOOTER"))), "420.00");
    }

    @Test
    void calculatesVisualLayoutAndHomepageImages() {
        assertFees(service.calculateAutomaticFees("VISUAL", info("", "普通消息类"), authors(2)), "15.00", "15.00");

        List<FeeSubmitDTO.ContributorDTO> designers = authors(2);
        designers.get(0).setWorkCount(1);
        designers.get(1).setWorkCount(3);
        assertFees(service.calculateAutomaticFees("VISUAL", info("", "校园网首页大图"), designers), "40.00", "120.00");
    }

    @Test
    void acceptsBothVisualSvgCategoryNames() {
        assertFees(service.calculateAutomaticFees("VISUAL",
                info("", "互动、长图、SVG动图、手绘"), authors(1)), "50.00");
        assertFees(service.calculateAutomaticFees("VISUAL",
                info("", "互动、长图、SVG、手绘"), authors(1)), "50.00");
    }

    @Test
    void appliesSpecialNodeMultiplier() {
        Map<String, Object> info = info("微信", "原创类");
        info.put("readCount", 0);
        info.put("isSpecialNode", 1);
        info.put("specialMultiplier", 3);
        assertFees(service.calculateAutomaticFees("EDITORIAL", info, authors(1)), "90.00");
    }

    @Test
    void calculatesMonthlySubsidiesPerPerson() {
        assertFees(service.calculateAutomaticFees("SUBSIDY", info("", "主席团成员"), authors(2)), "150.00", "150.00");
        assertFees(service.calculateAutomaticFees("SUBSIDY", info("", "校园巡查组员"), authors(1)), "50.00");
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
