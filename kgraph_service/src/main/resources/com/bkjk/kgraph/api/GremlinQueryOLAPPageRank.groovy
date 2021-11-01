package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.sf.json.JSONObject
import org.apache.hadoop.hbase.CompareOperator
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.BinaryComparator
import org.apache.hadoop.hbase.filter.FamilyFilter
import org.apache.hadoop.hbase.filter.Filter
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.QualifierFilter
import org.apache.hadoop.hbase.filter.RowFilter
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.PluginService
import org.apache.tinkerpop.gremlin.structure.Graph
import com.bkjk.kgraph.hbase.src.HBaseService
import org.apache.hadoop.conf.Configuration
import javax.annotation.Resource


class GremlinQueryOLAPPageRank implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinQueryOLAPPageRank.class)

    @Resource
    HBaseService hBaseService

    List getPageRank(String tableName, String vid, String label, String edgeName) {

        String pageRankKey = "pr"
        if (label && edgeName) {
            pageRankKey = label + "_" + edgeName + "_pr"
        } else if(label) {
            pageRankKey = label + "_pr"
        } else if(edgeName) {
            pageRankKey = edgeName + "_pr"
        }

        List<Filter> filters = new ArrayList<Filter>()

        Filter filter = new RowFilter(CompareOperator.EQUAL, new BinaryComparator(vid.getBytes()))
        filters.add(filter)

        filter = new FamilyFilter(CompareOperator.EQUAL, new BinaryComparator("page_rank" as byte[]))
        filters.add(filter)

        filter = new QualifierFilter(CompareOperator.EQUAL, new BinaryComparator(pageRankKey.getBytes()))
        filters.add(filter)

        FilterList filterList = new FilterList(filters)

        Scan scan = new Scan()
        scan.setFilter(filterList)

        Map results = hBaseService.queryData(tableName, scan)

        logger.info(results as String)

        return results.entrySet().toList()
    }

    List run(JSONObject params) {
        Graph graph
        String graphName = ""

        try {
            Configuration conf = new Configuration()
            logger.info("fs.defaultFS: "+ conf.get("fs.defaultFS"))

            graphName = params.getString("graph").trim()

            String vid
            if (params.containsKey("vid")) {
                vid = params.getString("vid")
            }

            if (vid == null) {
                throw new RuntimeException("Param error: node vid is missing")
            }

            def label = null
            if (params.containsKey("label")) {
                label = params.getString("label").trim()
            }

            def edgeName = null
            if (params.containsKey("edge_name")) {
                edgeName = params.getString("edge_name").trim()
            }

            String tableName = "kgraph:" + graphName + "_page_rank"

            List results = getPageRank(tableName, vid, label, edgeName)

            logger.info(results as String)

            return results
        } catch (Exception e) {
            logger.error("Fail to query page rank for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinQueryOLAPPageRank.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
