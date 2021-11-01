package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.structure.Column
import org.janusgraph.core.attribute.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Autowired

class GremlinGetProperties implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinGetProperties.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim() + "_g"

            def label = null
            if (params.containsKey("label")) {
                label = params.getString("label").trim()
            } else {
                throw new RuntimeException("Param error: miss label")
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

            List data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                    range(startIdx, endIdx).valueMap().select(Column.keys).groupCount().toList()

            if (data[0].size() > 0) {
                return [data[0].keySet().toString()[2..-3]]
            } else {
                return []
            }
        } catch (Exception e) {
            logger.error("Fail to get properties: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetProperties.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}

