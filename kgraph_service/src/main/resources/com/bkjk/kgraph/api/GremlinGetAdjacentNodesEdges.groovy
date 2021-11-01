package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class GremlinGetAdjacentNodesEdges implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinGetAdjacentNodesEdges.class)

    @Autowired
    GremlinDriver pool

    @Autowired
    @Qualifier("getAdjacentNodes")
    private PluginService adjacentNodes

    @Autowired
    @Qualifier("getAdjacentEdges")
    private PluginService adjacentEdges

    List run(JSONObject params) {

        try {
            def nodeData = adjacentNodes.run(params)
            def edgeData = adjacentEdges.run(params)

            return [nodeData, edgeData]
        } catch (Exception e) {
            logger.error("Fail to get adjacent nodes: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetAdjacentNodesEdges.class.toString(), e)
        }
    }

}




