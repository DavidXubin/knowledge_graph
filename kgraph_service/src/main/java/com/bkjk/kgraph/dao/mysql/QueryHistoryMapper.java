package com.bkjk.kgraph.dao.mysql;

import com.bkjk.kgraph.model.QueryHistory;
import com.bkjk.kgraph.model.QueryHistoryExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface QueryHistoryMapper {
    long countByExample(QueryHistoryExample example);

    int deleteByExample(QueryHistoryExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(QueryHistory record);

    int insertSelective(QueryHistory record);

    List<QueryHistory> selectByExampleWithRowbounds(QueryHistoryExample example, RowBounds rowBounds);

    List<QueryHistory> selectByExample(QueryHistoryExample example);

    QueryHistory selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") QueryHistory record, @Param("example") QueryHistoryExample example);

    int updateByExample(@Param("record") QueryHistory record, @Param("example") QueryHistoryExample example);

    int updateByPrimaryKeySelective(QueryHistory record);

    int updateByPrimaryKey(QueryHistory record);
}