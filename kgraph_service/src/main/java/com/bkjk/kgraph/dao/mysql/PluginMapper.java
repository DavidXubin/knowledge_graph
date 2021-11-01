package com.bkjk.kgraph.dao.mysql;

import com.bkjk.kgraph.model.Plugin;
import com.bkjk.kgraph.model.PluginExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface PluginMapper {
    long countByExample(PluginExample example);

    int deleteByExample(PluginExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(Plugin record);

    int insertSelective(Plugin record);

    List<Plugin> selectByExampleWithRowbounds(PluginExample example, RowBounds rowBounds);

    List<Plugin> selectByExample(PluginExample example);

    Plugin selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") Plugin record, @Param("example") PluginExample example);

    int updateByExample(@Param("record") Plugin record, @Param("example") PluginExample example);

    int updateByPrimaryKeySelective(Plugin record);

    int updateByPrimaryKey(Plugin record);
}