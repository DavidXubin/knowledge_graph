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


class GremlinQueryOLAPConnectedComponent implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinQueryOLAPConnectedComponent.class)

    //@Resource
    //HBaseService hBaseService

    List getConnectedComponent(String tableName, String vid, String label, String edgeName) {

        String componentKey = "component"
        if (label && edgeName) {
            componentKey = label + "_" + edgeName + "_component"
        } else if(label) {
            componentKey = label + "_component"
        } else if(edgeName) {
            componentKey = edgeName + "_component"
        }

        List<Filter> filters = new ArrayList<Filter>()

        Filter filter = new RowFilter(CompareOperator.EQUAL, new BinaryComparator(vid.getBytes()))
        filters.add(filter)

        filter = new FamilyFilter(CompareOperator.EQUAL, new BinaryComparator("connected_component" as byte[]))
        filters.add(filter)

        filter = new QualifierFilter(CompareOperator.EQUAL, new BinaryComparator(componentKey.getBytes()))
        filters.add(filter)

        FilterList filterList = new FilterList(filters)

        Scan scan = new Scan()
        scan.setFilter(filterList)

        //Map results = hBaseService.queryData(tableName, scan)

        //logger.info(results as String)

        //return results.entrySet().toList()

        return []
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

            String tableName = "kgraph:" + graphName + "_connected_component"

            List results = getConnectedComponent(tableName, vid, label, edgeName)

            logger.info(results as String)

            return results
        } catch (Exception e) {
            logger.error("Fail to query connected components for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinQueryOLAPConnectedComponent.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
