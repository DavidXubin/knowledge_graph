package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.process.traversal.Pop
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Autowired

class GremlinGetAdjacentNodes implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinGetAdjacentNodes.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim() + "_g"

            def nodeId = null
            if (params.containsKey("node_id")) {
                nodeId = params.getString("node_id").trim()
            } else {
                throw new RuntimeException("Param error: miss node id")
            }

            def edge = null
            if (params.containsKey("edge")) {
                edge = params.getString("edge").trim()
            }

            int startIdx = 0
            if (params.containsKey("start_idx")) {
                startIdx = params.getInt("start_idx")
            }

            int limit = pool.getPageSize()
            if (params.containsKey("limit")) {
                limit = params.getInt("limit")
                if (limit > pool.getPageSize()) {
                    limit = pool.getPageSize()
                }
            }

            int endIdx = startIdx + limit

            g = pool.get(user, graph)

            def data
            if (edge == null || edge.trim().size() == 0) {
                data = g.V(nodeId).both().as("node").select(Pop.all,"node")
                        .unfold().range(startIdx, endIdx).elementMap().toList()
            } else {
                data = g.V(nodeId).both(edge).as("node").select(Pop.all,"node")
                        .unfold().range(startIdx, endIdx).elementMap().toList()
            }

            data.add(g.V(nodeId).elementMap().toList()[0])

            return data
        } catch (Exception e) {
            logger.error("Fail to get adjacent nodes: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetAdjacentNodes.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}


