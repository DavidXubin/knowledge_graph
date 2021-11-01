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
import org.apache.tinkerpop.gremlin.process.computer.traversal.step.map.ConnectedComponent
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.Resource


class GremlinOLAPConnectedComponent implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinOLAPConnectedComponent.class)

    @Autowired
    GremlinDriver pool

    @Resource
    HBaseService hBaseService

    PluginProperties properties = new PluginProperties()

    static private Map columnFamily = [
            "vertex": ["vid"],
            "connected_component": ["component"]
    ]

    void persist(List results, String graphName, String label, String edgeName) {

        String tableName = "kgraph:" + graphName + "_connected_component"
        List<String> families = columnFamily.keySet().toList()

        hBaseService.createTable(tableName, families)

        int batchSize = 10000
        int currentSize = 0

        Map<String, Map<String, Object>> batchData = new HashMap<>()

        String componentKey = "component"
        if (label && edgeName) {
            componentKey = label + "_" + edgeName + "_component"
        } else if(label) {
            componentKey = label + "_component"
        } else if(edgeName) {
            componentKey = edgeName + "_component"
        }

        Map<String, String> inverseColumnFamily = new HashMap<>()

        inverseColumnFamily.put(componentKey, "connected_component")

        for (Map element: (results as List<Map>)) {

            logger.info(element as String)

            String vertexId = element.get(T.id)

            logger.info(element.get(T.id) as String)

            String value = (String)element.get(componentKey)

            //logger.info(value as String)

            Map<String, Object> data = new HashMap<>()
            data.put(componentKey, value)

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
            String componentAias = "component"

            if (label && edgeName) {
                componentAias = label + "_" + edgeName + '_component'

                results = g.V().hasLabel(label).connectedComponent().with(ConnectedComponent.edges, __.outE(edgeName)).
                        with(ConnectedComponent.propertyName, componentAias).elementMap(componentAias).toList()

            } else if(label) {
                componentAias = label + '_component'

                results = g.V().hasLabel(label).connectedComponent().with(ConnectedComponent.propertyName, componentAias).
                        elementMap(componentAias).toList()
            } else if(edgeName) {
                componentAias = edgeName + '_component'

                results = g.V().connectedComponent().with(ConnectedComponent.edges, __.outE(edgeName)).
                        with(ConnectedComponent.propertyName, componentAias).elementMap(componentAias).toList()
            } else {
                results = g.V().connectedComponent().with(ConnectedComponent.propertyName, componentAias).elementMap(componentAias).toList()
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

            logger.error("Fail to build connected components for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinOLAPConnectedComponent.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
