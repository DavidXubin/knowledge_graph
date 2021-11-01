package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.hbase.src.HBaseService
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginProperties
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.hadoop.conf.Configuration
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer
import org.apache.tinkerpop.gremlin.process.computer.traversal.step.map.PageRank
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.Resource

class GremlinOLAPPageRank implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinOLAPPageRank.class)

    @Autowired
    GremlinDriver pool

    @Resource
    HBaseService hBaseService

    PluginProperties properties = new PluginProperties()

    static private Map columnFamily = [
        "vertex": ["vid"],
        "page_rank": ["pr"]
    ]

    void persist(List results, String graphName, String label, String edgeName) {

        String tableName = "kgraph:" + graphName + "_page_rank"
        List<String> families = columnFamily.keySet().toList()

        hBaseService.createTable(tableName, families)

        int batchSize = 10000
        int currentSize = 0

        Map<String, Map<String, Object>> batchData = new HashMap<>()

        String pageRankKey = "pr"
        if (label && edgeName) {
            pageRankKey = label + "_" + edgeName + "_pr"
        } else if(label) {
            pageRankKey = label + "_pr"
        } else if(edgeName) {
            pageRankKey = edgeName + "_pr"
        }

        Map<String, String> inverseColumnFamily = new HashMap<>()

        inverseColumnFamily.put(pageRankKey, "page_rank")

        for (Map element: (results as List<Map>)) {

            logger.info(element as String)

            String vertexId = element.get(T.id)

            logger.info(element.get(T.id) as String)

            Float value = (Float)element.get(pageRankKey)

            logger.info(value as String)

            Map<String, Object> data = new HashMap<>()
            data.put(pageRankKey, value)

            batchData.put(vertexId, data)

            currentSize += 1

            if (currentSize >= batchSize) {
                hBaseService.addBatchRecords(tableName, batchData, inverseColumnFamily)

                currentSize = 0
                batchData.clear()
            }
        }

        if (batchData.size() > 0) {
            hBaseService.addBatchRecords(tableName, batchData, inverseColumnFamily)
        }
    }

    List run(JSONObject params) {
        Graph graph
        String graphName = ""

        try {
            properties.setStatus(PluginProperties.Status.RUNNING)

            System.setProperty("HADOOP_GREMLIN_LIBS", pool.getOLAPLibPath())
            //System.setProperty("hadoop.home.dir", "/data/software/hadoop-2.9.1/")

            Configuration conf = new Configuration()
            logger.info("fs.defaultFS: " + conf.get("fs.defaultFS"))

            graphName = params.getString("graph").trim()
            graph = GraphFactory.open(pool.getOLAPConfRootPath() + graphName + "-janusgraph-hbase-server.properties")
            logger.info("Open graph[{}]", graphName)

            def label = null
            if (params.containsKey("label")) {
                label = params.getString("label").trim()
            }

            def edgeName = null
            if (params.containsKey("edge_name")) {
                edgeName = params.getString("edge_name").trim()
            }

            def g = graph.traversal().withComputer(SparkGraphComputer)

            List results = []
            String pgAias = "pr"

            if (label && edgeName) {
                pgAias = label + "_" + edgeName + '_pr'

                results = g.V().hasLabel(label).pageRank().with(PageRank.edges, __.outE(edgeName)).
                        with(PageRank.propertyName, pgAias).elementMap(pgAias).toList()
            } else if(label) {
                pgAias = label + '_pr'

                results = g.V().hasLabel(label).pageRank().with(PageRank.propertyName, pgAias).elementMap(pgAias).toList()
            } else if(edgeName) {
                pgAias = edgeName + '_pr'

                results = g.V().pageRank().with(PageRank.edges, __.outE(edgeName)).
                        with(PageRank.propertyName, pgAias).elementMap(pgAias).toList()
            } else {
                results = g.V().pageRank().with(PageRank.propertyName, pgAias).elementMap(pgAias).toList()
            }

            if (results.empty) {
                return []
            }

            logger.info(results as String)

            persist(results, graphName, label, edgeName)

            properties.setStatus(PluginProperties.Status.SUCCESS)

            return results

        } catch (Exception e) {
            properties.setStatus(PluginProperties.Status.FAILURE)

            logger.error("Fail to get shortest path for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinOLAPPageRank.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
