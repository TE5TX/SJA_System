package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import com.sja.system.dal.mysql.BizPhotographyWorkMapper;
import com.sja.system.dal.mysql.BizVideoWorkMapper;
import com.sja.system.dal.mysql.BizVisualWorkMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 未发布作品隐藏发布日期的持久化回归测试。
 */
// 202609281554 - 验证四类作品统一以数据库创建时间填写未发布任务的发布日期。
class UnpublishedFeeWorkDateTest {

    @Test
    void editorialUsesCreationDate() {
        BizEditorialArticleMapper mapper = mock(BizEditorialArticleMapper.class);
        new EditorialFeeWorkAdapter(mapper).assignCreationDateToUnpublishedWork(11L);
        assertCreationDateUpdate(mapper);
    }

    @Test
    void photographyUsesCreationDate() {
        BizPhotographyWorkMapper mapper = mock(BizPhotographyWorkMapper.class);
        new PhotographyFeeWorkAdapter(mapper).assignCreationDateToUnpublishedWork(12L);
        assertCreationDateUpdate(mapper);
    }

    @Test
    void visualUsesCreationDate() {
        BizVisualWorkMapper mapper = mock(BizVisualWorkMapper.class);
        new VisualFeeWorkAdapter(mapper).assignCreationDateToUnpublishedWork(13L);
        assertCreationDateUpdate(mapper);
    }

    @Test
    void videoUsesCreationDate() {
        BizVideoWorkMapper mapper = mock(BizVideoWorkMapper.class);
        new VideoFeeWorkAdapter(mapper).assignCreationDateToUnpublishedWork(14L);
        assertCreationDateUpdate(mapper);
    }

    /** 校验数据库侧以 DATE(create_time) 赋值，避免客户端或消费者所在机器时间不一致。 */
    private <T> void assertCreationDateUpdate(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper) {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<UpdateWrapper> dateUpdate = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(mapper).update(isNull(), dateUpdate.capture());
        assertTrue(dateUpdate.getValue().getSqlSet().contains("publish_date = DATE(create_time)"));
    }
}
