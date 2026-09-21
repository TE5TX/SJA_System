package com.sja.system.mapper;

import com.sja.system.mapper.model.FeeListQuery;
import com.sja.system.mapper.model.FeeListRow;
import com.sja.system.mapper.provider.FeeQuerySqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * 稿费审核列表和导出的跨表只读查询 Mapper。
 */
@Mapper
public interface FeeQueryMapper {

    /** 查询符合条件的稿费任务总数。 */
    @SelectProvider(type = FeeQuerySqlProvider.class, method = "countFeeItems")
    long countFeeItems(@Param("query") FeeListQuery query);

    /** 按统一排序规则读取指定区间的稿费任务。 */
    @SelectProvider(type = FeeQuerySqlProvider.class, method = "selectFeeItems")
    List<FeeListRow> selectFeeItems(@Param("query") FeeListQuery query,
                                    @Param("offset") long offset,
                                    @Param("limit") int limit);
}
