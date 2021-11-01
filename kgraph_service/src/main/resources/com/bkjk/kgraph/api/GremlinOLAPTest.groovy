package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Autowired
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T


class GremlinOLAPTest implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinOLAPTest.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        Graph graph

        try {
            System.setProperty("HADOOP_GREMLIN_LIBS", pool.getOLAPLibPath())

            String graphName = params.getString("graph").trim()

            graph = GraphFactory.open(pool.getOLAPConfRootPath() + graphName + "-janusgraph-hbase-server.properties")

            GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class)

            long count = g.V().count().next()
            logger.info(count + "-----g.V().count()-------------------------------")

            long count_E = g.E().count().next()
            logger.info(count_E + "-----g.E().count()-------------------------------")

            Map verts = g.V().groupCount().by(T.label).next()
            logger.info("-------Vertices group counts: " + verts)

            return [["node count": count, "edge_count": count_E]]
        } catch (Exception e) {
            logger.error("Fail to get cluster graph names: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinOLAPTest.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
