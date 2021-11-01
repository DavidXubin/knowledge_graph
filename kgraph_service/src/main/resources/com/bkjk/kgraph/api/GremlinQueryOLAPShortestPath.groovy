package com.bkjk.kgraph.api

import com.bkjk.kgraph.hbase.src.HBaseService
import org.apache.hadoop.hbase.CompareOperator
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.Filter
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.filter.RowFilter
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.hadoop.conf.Configuration

import javax.annotation.Resource


class GremlinQueryOLAPShortestPath implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinQueryOLAPShortestPath.class)

    @Resource
    HBaseService hBaseService

    static private Map columnFamily = [
        "vertex_pairs": ["start", "end"],
        "shortest_path": ["path"]
    ]

    List getSingleVertexAllPath(String tableName, String vid) {
        Map results = hBaseService.getRegexRecords(tableName, vid)

        return results.entrySet().toList()
    }

    List getPairedVertexPath(String tableName, String startVid, String endVid) {

        List<Filter> filters = new ArrayList<Filter>()

        Filter filter = new RowFilter(CompareOperator.EQUAL, new RegexStringComparator("^" + startVid + "_"))

        filters.add(filter)

        filter = new SingleColumnValueFilter("vertex_pairs" as byte[], "start" as byte[],
                CompareOperator.EQUAL, startVid.getBytes())

        filters.add(filter)

        filter = new SingleColumnValueFilter("vertex_pairs" as byte[], "end" as byte[],
                CompareOperator.EQUAL,  endVid.getBytes())

        filters.add(filter)

        FilterList filterList = new FilterList(filters)

        Scan scan = new Scan()
        scan.setFilter(filterList)

        Map results = hBaseService.queryData(tableName, scan)

        return results.entrySet().toList()
    }

    List run(JSONObject params) {
        Graph graph
        String graphName = ""

        try {

            Configuration conf = new Configuration()
            logger.info("fs.defaultFS: "+ conf.get("fs.defaultFS"))

            graphName = params.getString("graph").trim()

            String startVid
            if (params.containsKey("start_vid")) {
                startVid = params.getString("start_vid")
            }

            if (startVid == null) {
                throw new RuntimeException("Param error: start_vid is missing")
            }

            String endVid
            if (params.containsKey("end_vid")) {
                endVid = params.getString("end_vid")
            }

            List shortestPaths = []
            String tableName = "kgraph:" + graphName + "_shortest_path"

            if (startVid == endVid || endVid == null) {
                shortestPaths = getSingleVertexAllPath(tableName, startVid)
            } else {
                shortestPaths = getPairedVertexPath(tableName, startVid, endVid)
            }

            logger.info(shortestPaths as String)

            return shortestPaths
        } catch (Exception e) {
            logger.error("Fail to query shortest path for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinQueryOLAPShortestPath.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}