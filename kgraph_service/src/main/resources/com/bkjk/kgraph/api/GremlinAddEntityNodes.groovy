package com.bkjk.kgraph.api
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import com.bkjk.kgraph.utils.Toolkit
import net.sf.json.JSONObject
import org.janusgraph.core.JanusGraphVertex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
//import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.janusgraph.core.JanusGraphTransaction
import org.janusgraph.core.JanusGraph
import org.janusgraph.core.JanusGraphFactory

/*
 *在graph的配置文件中添加以下配置项，可以在添加节点和关系时根据预定义的schema做约束
 *schema.constraints = true
 *schema.default = none
*/
class GremlinAddEntityNodes implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinAddEntityNodes.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        JanusGraph graph
        //GraphTraversalSource g
        JanusGraphTransaction graphTransaction

        try {
            String graphName = params.getString("graph").trim()

            List<JSONObject> entities
            if (params.containsKey("entities")) {
                entities = params.getJSONArray("entities")
            } else {
                throw new RuntimeException("Param error: miss entities")
            }

            //g = pool.get(user, graph)
            graph = JanusGraphFactory.open(pool.getConfRootPath() + graphName + "-janusgraph-hbase-server.properties")

            graphTransaction = graph.newTransaction()

            for (JSONObject entity in (entities as List<JSONObject>)) {
                Iterator<String> keys = entity.keys()

                JanusGraphVertex nodeV

                while (keys.hasNext()) {
                    String name = keys.next()
                    def value = entity.optString(name)
                    if (name == "label") {
                        nodeV = graphTransaction.addVertex(value)
                    } else {
                        if (!nodeV.properties(name).hasNext()) {
                            Object convertedValue = Toolkit.convertPropertyValue(value, graphTransaction.getPropertyKey(name).dataType())

                            nodeV.property(name, convertedValue)
                        }
                    }
                }
            }

            graphTransaction.commit()

            return [["result": "added", "entity_nodes_added": entities.size()]]
        } catch (Exception e) {
            if (graphTransaction) {
                graphTransaction.rollback()
            }

            logger.error("Fail to add nodes: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinAddEntityNodes.class.toString(), e)
        } finally {
            //pool.release(user, graph, g)
            if (graphTransaction) {
                graphTransaction.close()
            }

            if (graph) {
                graph.close()
            }
        }
    }

}
